package org.maurodata.visitor.common

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler

/**
 * A visitor that re-establishes all parent-child back-references, cross-references, and
 * structural links across the Mauro domain object graph.
 * <p>
 * This visitor consolidates the logic previously spread across individual {@code setAssociations()}
 * methods on each domain class:
 * <ul>
 *   <li>{@link AdministeredItem} – sets {@code multiFacetAwareItem} on all facets
 *       and back-links on summary-metadata reports and rule representations.</li>
 *   <li>{@link org.maurodata.domain.model.Model} – additionally sets
 *       {@code multiFacetAwareItem} on version links.</li>
 *   <li>{@link Folder} – wires {@code parentFolder} / {@code folder} onto
 *       contained child folders and models.</li>
 *   <li>{@link DataModel} – sets {@code parent}/{@code dataModel} on data types,
 *       resolves data-class hierarchy and data-element/data-type cross-references.</li>
 *   <li>{@link DataType} – sets {@code enumerationType} on enumeration values
 *       and registers them on the owning data model.</li>
 *   <li>{@link Annotation} – sets {@code parentAnnotation} on child annotations.</li>
 *   <li>{@link Terminology} – wires term/relationship-type/relationship
 *       back-references and resolves relationship cross-references.</li>
 *   <li>{@link ClassificationScheme} – wires {@code classificationScheme} and
 *       {@code parentClassifier} onto the classifier tree.</li>
 *   <li>{@link org.maurodata.domain.dataflow.DataFlow} – sets {@code dataFlow}
 *       and {@code dataClassComponent} back-references on flow components.</li>
 * </ul>
 * <p>
 * <b>Performance notes:</b>
 * <ul>
 *   <li>Each node in the domain graph is visited exactly once.</li>
 *   <li>The class is {@code @CompileStatic}: all property accesses and method calls inside
 *       handler closures compile to direct {@code invokevirtual}/{@code invokeinterface}
 *       bytecode, eliminating Groovy's {@code IndyInterface} dynamic-dispatch overhead.</li>
 *   <li>Per-{@link DataModel} context (data-type lookup map, reference-type list) is stored in
 *       {@code IdentityHashMap} instance variables and cleaned up in {@code onLeave(DataModel)}.</li>
 * </ul>
 * Do not share a single {@code SetAssociationsVisitor} instance across concurrent threads.
 * <p>
 * Usage:
 * <pre>
 *   new SetAssociationsVisitor().visitFolder(rootFolder)
 *   // or for a standalone model:
 *   new SetAssociationsVisitor().visitDataModel(dataModel)
 * </pre>
 */
@Slf4j
@CompileStatic
class SetAssociationsVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    // The traversal state (visited sets, per-model lookups) is per-visitor, so share one visitor per thread.
    private static final ThreadLocal<SetAssociationsVisitor> VISITOR = ThreadLocal.withInitial { new SetAssociationsVisitor() }

    static void setAssociations(AdministeredItem item) {
        SetAssociationsVisitor visitor = VISITOR.get()
        visitor.resetTraversalState()
        try {
            item.accept(visitor)
        } finally {
            visitor.resetTraversalState()
        }
    }

    private static class ModelContext {
        Map<UUID, DataType> dataTypesById = new HashMap<>()
        Map<String, DataType> dataTypesByLabel = new HashMap<>()
        Set<DataElement> knownElements = Collections.newSetFromMap(new IdentityHashMap<DataElement, Boolean>())
        Set<DataClass> knownClasses = Collections.newSetFromMap(new IdentityHashMap<DataClass, Boolean>())

        ModelContext(DataModel dataModel) {
            for (DataType dt : dataModel.dataTypes) {
                if (dt.id) {
                    dataTypesById.putIfAbsent(dt.id, dt)
                }
                dataTypesByLabel.putIfAbsent(dt.label, dt)
            }
            knownElements.addAll(dataModel.dataElements)
            knownClasses.addAll(dataModel.allDataClasses)
        }
    }

    private final Map<DataModel, ModelContext> contexts = new IdentityHashMap<>()

    @Override
    void resetTraversalState() {
        super.resetTraversalState()
        contexts.clear()
    }

    private ModelContext contextFor(DataModel dataModel) {
        ModelContext context = contexts.get(dataModel)
        if (context == null) {
            context = new ModelContext(dataModel)
            contexts.put(dataModel, context)
        }
        context
    }

    SetAssociationsVisitor() {
        addHandler(this)
    }

    // Direct dispatch (rather than registered closures) avoids Groovy closure-call overhead per node.
    // Order matches the original handler registration order.
    @Override
    void enter(Item item) {
        if (item instanceof AdministeredItem) {
            enterAdministeredItem((AdministeredItem) item)
        }
        if (item instanceof Rule) {
            for (RuleRepresentation rr : ((Rule) item).ruleRepresentations) {
                rr.rule = (Rule) item
            }
        }
        if (item instanceof SummaryMetadata) {
            SummaryMetadata summaryMetadata = (SummaryMetadata) item
            for (SummaryMetadataReport report : summaryMetadata.summaryMetadataReports) {
                report.summaryMetadata = summaryMetadata
            }
        }
        if (item instanceof Annotation) {
            Annotation annotation = (Annotation) item
            for (Annotation child : annotation.childAnnotations) {
                child.parentAnnotation = annotation
            }
        }
        if (item instanceof Model) {
            Model model = (Model) item
            for (VersionLink versionLink : model.versionLinks) {
                versionLink.multiFacetAwareItem = model
            }
        }
        if (item instanceof Folder) {
            enterFolder((Folder) item)
        }
        if (item instanceof DataModel) {
            DataModel dataModel = (DataModel) item
            for (DataType dt : dataModel.dataTypes) {
                dt.dataModel = dataModel
            }
            for (DataClass dc : dataModel.dataClasses) {
                dc.dataModel = dataModel
            }
            for (DataComponent component : dataModel.dataComponents) {
                component.dataModel = dataModel
            }
        }
        if (item instanceof DataClass) {
            enterDataClass((DataClass) item)
        }
        if (item instanceof DataElement) {
            enterDataElement((DataElement) item)
        }
        if (item instanceof DataType) {
            enterDataType((DataType) item)
        }
        if (item instanceof Terminology) {
            enterTerminology((Terminology) item)
        }
        if (item instanceof ClassificationScheme) {
            for (Classifier classifier : ((ClassificationScheme) item).csClassifiers) {
                classifier.classificationScheme = (ClassificationScheme) item
            }
        }
        if (item instanceof Classifier) {
            enterClassifier((Classifier) item)
        }
        if (item instanceof DataFlow) {
            for (DataClassComponent dcc : ((DataFlow) item).dataClassComponents) {
                dcc.dataFlow = (DataFlow) item
            }
        }
        if (item instanceof DataClassComponent) {
            for (DataElementComponent dec : ((DataClassComponent) item).dataElementComponents) {
                dec.dataClassComponent = (DataClassComponent) item
            }
        }
    }

    @Override
    void leave(Item item) {
        if (item instanceof DataModel) {
            leaveDataModel((DataModel) item)
        }
    }

    private static void enterAdministeredItem(AdministeredItem item) {
        if (item.edits) { for (Edit f : item.edits) { f.multiFacetAwareItem = item } }
        if (item.metadata) { for (Metadata f : item.metadata) { f.multiFacetAwareItem = item } }
        if (item.summaryMetadata) { for (SummaryMetadata f : item.summaryMetadata) { f.multiFacetAwareItem = item } }
        if (item.rules) { for (Rule f : item.rules) { f.multiFacetAwareItem = item } }
        if (item.annotations) { for (Annotation f : item.annotations) { f.multiFacetAwareItem = item } }
        if (item.referenceFiles) { for (ReferenceFile f : item.referenceFiles) { f.multiFacetAwareItem = item } }
        if (item.semanticLinks) { for (SemanticLink f : item.semanticLinks) { f.multiFacetAwareItem = item } }
        if (item.itemLinkSources) { for (ItemLink f : item.itemLinkSources) { f.multiFacetAwareItem = item } }
    }

    private static void enterFolder(Folder folder) {
        for (Folder f : folder.childFolders) { f.parentFolder = folder }
        for (DataModel f : folder.dataModels) { f.folder = folder }
        for (Terminology f : folder.terminologies) { f.folder = folder }
        for (CodeSet f : folder.codeSets) { f.folder = folder }
        for (ClassificationScheme f : folder.classificationSchemes) { f.folder = folder }
    }

    private void enterDataClass(DataClass dataClass) {
        DataModel dataModel = dataClass.dataModel
        if (dataModel) {
            if (contextFor(dataModel).knownClasses.add(dataClass)) {
                dataModel.allDataClasses.add(dataClass)
            }
        }
        for (DataClass child : dataClass.dataClasses) {
            child.parentDataClass = dataClass
            child.dataModel = dataModel
        }
        for (DataElement element : dataClass.dataElements) {
            element.dataClass = dataClass
            element.dataModel = dataModel
        }
    }

    private void enterDataElement(DataElement dataElement) {
        DataModel dataModel = dataElement.dataModel
        if (dataModel) {
            ModelContext context = contextFor(dataModel)
            if (context.knownElements.add(dataElement)) {
                dataModel.dataElements.add(dataElement)
            }

            if (dataElement.dataType) {
                DataType resolvedType = dataElement.dataType.id ?
                                        context.dataTypesById.get(dataElement.dataType.id) :
                                        context.dataTypesByLabel.get(dataElement.dataType.label)

                if (resolvedType) {
                    dataElement.dataType = resolvedType
                } else {
                    log.error(
                        'SetAssociationsVisitor onEnter(DataElement) failed to find a DataType ' +
                        "for id=${dataElement.dataType?.id} or label=${dataElement.dataType?.label}")
                }
            }
        }
    }

    private static void enterDataType(DataType dataType) {
        if (!dataType.enumerationValues) {
            return
        }
        DataModel dataModel = dataType.dataModel
        for (EnumerationValue ev : dataType.enumerationValues) {
            ev.enumerationType = dataType
            if (dataModel) {
                dataModel.enumerationValues.add(ev)
                ev.dataModel = dataModel
            }
        }
    }

    private static void enterTerminology(Terminology terminology) {
        Map<Object, Term> termsMap = new HashMap<Object, Term>()
        for (Term t : terminology.terms) { termsMap.put(t.id ?: t.code, t) }

        Map<Object, TermRelationshipType> termRelTypesMap = new HashMap<Object, TermRelationshipType>()
        for (TermRelationshipType trt : terminology.termRelationshipTypes) { termRelTypesMap.put(trt.id ?: trt.label, trt) }

        for (Term term : terminology.terms) { term.parent = terminology }
        for (TermRelationshipType trt : terminology.termRelationshipTypes) { trt.parent = terminology }
        for (TermRelationship rel : terminology.termRelationships) {
            rel.parent = terminology
            if (rel.relationshipType) {
                rel.relationshipType = termRelTypesMap.get(rel.relationshipType.id ?: rel.relationshipType.label)
            }
            if (rel.sourceTerm) {
                rel.sourceTerm = termsMap.get(rel.sourceTerm.id ?: rel.sourceTerm.code)
            }
            if (rel.targetTerm) {
                rel.targetTerm = termsMap.get(rel.targetTerm.id ?: rel.targetTerm.code)
            }
        }
    }

    private static void enterClassifier(Classifier classifier) {
        classifier.parent = (classifier.parentClassifier ?: classifier.classificationScheme) as AdministeredItem
        for (Classifier child : classifier.childClassifiers) {
            child.parentClassifier = classifier
            child.classificationScheme = classifier.classificationScheme
            child.parent = classifier
        }
    }

    private void leaveDataModel(DataModel dataModel) {
        contexts.remove(dataModel)

        Map<UUID, DataClass> classesById = new HashMap<>()
        Map<String, DataClass> classesByLabel = new HashMap<>()
        for (DataClass dc : dataModel.allDataClasses) {
            if (dc.id) {
                classesById.putIfAbsent(dc.id, dc)
            }
            classesByLabel.putIfAbsent(dc.label, dc)
        }

        for (DataType dataType : dataModel.dataTypes) {
            if (dataType.referenceType) {
                DataClass refClass = dataType.referenceClass.id ?
                                     classesById.get(dataType.referenceClass.id) :
                                     classesByLabel.get(dataType.referenceClass.label)

                if (refClass) {
                    dataType.referenceClass = refClass
                } else {
                    log.error(
                        'SetAssociationsVisitor onLeave(DataModel) failed to find a DataClass ' +
                        "for referenceClass id=${dataType.referenceClass?.id} or label=${dataType.referenceClass?.label}")
                }
            }
        }

        for (DataClass dataClass : dataModel.allDataClasses) {
            if (dataClass.extendsDataClasses) {
                List<DataClass> resolvedExtends = []
                for (DataClass superClass : dataClass.extendsDataClasses) {
                    DataClass found = superClass.id ? classesById.get(superClass.id) : classesByLabel.get(superClass.label)
                    if (found) {
                        resolvedExtends.add(found)
                    } else {
                        log.error(
                            'SetAssociationsVisitor onLeave(DataModel) failed to find a DataClass ' +
                            "for id=${superClass.id} or label=${superClass.label}")
                    }
                }
                dataClass.extendsDataClasses = resolvedExtends
            }
        }
    }
}
