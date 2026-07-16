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
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.visitor.GenericDomainTraversalVisitor

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
class SetAssociationsVisitor extends GenericDomainTraversalVisitor {

    /**
     * Per-DataModel lookup maps stored while the visitor is inside a DataModel subtree.
     * Using IdentityHashMap so that two DataModel instances that happen to be equal by value
     * are still treated as distinct entries.
     */
    private final Map<DataModel, Map<String, DataType>> dataTypesMapByModel  = new IdentityHashMap<>()
    private final Map<DataModel, List<DataType>>         referenceTypesByModel = new IdentityHashMap<>()

    SetAssociationsVisitor() {

        // -----------------------------------------------------------------------
        // AdministeredItem – set multiFacetAwareItem on every facet.
        //
        // Each collection is iterated directly (no intermediate List<Facet>)
        // so that @CompileStatic can resolve the setter call via the concrete
        // element type rather than via a dynamic Object reference.
        //
        // Mirrors: AdministeredItem.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(AdministeredItem) { AdministeredItem item ->
            item.edits?.each           { it.multiFacetAwareItem = item }
            item.metadata?.each        { it.multiFacetAwareItem = item }
            item.summaryMetadata?.each { it.multiFacetAwareItem = item }
            item.rules?.each           { it.multiFacetAwareItem = item }
            item.annotations?.each     { it.multiFacetAwareItem = item }
            item.referenceFiles?.each  { it.multiFacetAwareItem = item }
            item.semanticLinks?.each   { it.multiFacetAwareItem = item }
        }

        // -----------------------------------------------------------------------
        // Rule – set rule back-reference on each RuleRepresentation.
        // Mirrors the rule-block inside AdministeredItem.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Rule) { Rule rule ->
            rule.ruleRepresentations?.each { ruleRepresentation ->
                ruleRepresentation.rule = rule
            }
        }

        // -----------------------------------------------------------------------
        // SummaryMetadata – set summaryMetadata back-reference on each report.
        // Mirrors the summaryMetadata-block inside AdministeredItem.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(SummaryMetadata) { SummaryMetadata summaryMetadata ->
            summaryMetadata.summaryMetadataReports?.each { report ->
                report.summaryMetadata = summaryMetadata
            }
        }

        // -----------------------------------------------------------------------
        // Annotation – set parentAnnotation on each child annotation.
        // Mirrors: Annotation.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Annotation) { Annotation annotation ->
            annotation.childAnnotations?.each { Annotation child ->
                child.parentAnnotation = annotation
            }
        }

        // -----------------------------------------------------------------------
        // Model – set multiFacetAwareItem on versionLinks.
        // Mirrors: Model.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Model) { Model model ->
            model.versionLinks?.each { versionLink ->
                versionLink.multiFacetAwareItem = model
            }
        }

        // -----------------------------------------------------------------------
        // Folder – wire parentFolder onto child folders and folder onto
        // the contained models/terminologies/codeSets/classificationSchemes.
        // Mirrors: Folder.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Folder) { Folder folder ->
            folder.childFolders?.each          { childFolder -> childFolder.parentFolder = folder }
            folder.dataModels?.each            { dataModel   -> dataModel.folder   = folder }
            folder.terminologies?.each         { terminology -> terminology.folder  = folder }
            folder.codeSets?.each              { codeSet     -> codeSet.folder      = folder }
            folder.classificationSchemes?.each { cs          -> cs.folder           = folder }
        }

        // -----------------------------------------------------------------------
        // DataModel (enter) – build per-model lookup state, wire DataType
        // back-references, and seed dataModel on the top-level DataClasses so
        // that onEnter(DataClass) can access it without a recursive helper.
        //
        // NOTE: the DataClass/DataElement tree is NOT walked here; that work is
        // distributed across onEnter(DataClass), onLeave(DataClass), and
        // onEnter(DataElement) so each node is visited exactly once by the
        // traversal rather than once here and once by the traversal (double work).
        //
        // Mirrors: DataModel.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataModel) { DataModel dataModel ->
            // Build a lookup map: id (as String) → DataType, and label → DataType
            Map<String, DataType> dataTypesMap = new HashMap<String, DataType>()
            dataModel.dataTypes.each { DataType dt ->
                if (dt.id)    dataTypesMap.put(dt.id.toString(), dt)
                if (dt.label) dataTypesMap.put(dt.label, dt)
            }
            dataTypesMapByModel.put(dataModel,  dataTypesMap)
            referenceTypesByModel.put(dataModel, dataModel.dataTypes.findAll { DataType dt -> dt.isReferenceType() })

            // Wire DataType back-references
            dataModel.dataTypes.each { DataType dataType ->
                dataType.parent    = dataModel
                dataType.dataModel = dataModel
            }

            // Seed dataModel on top-level DataClasses so onEnter(DataClass) can
            // read it without needing the parent as an explicit parameter.
            dataModel.dataClasses.each { DataClass dc ->
                dc.dataModel = dataModel
            }
        }

        // -----------------------------------------------------------------------
        // DataModel (leave) – resolve referenceClass on REFERENCE_TYPE DataTypes
        // now that allDataClasses has been fully populated by the traversal.
        // Clean up per-model state.
        // Mirrors: the post-loop referenceClass fix in DataModel.setAssociations()
        // -----------------------------------------------------------------------
        onLeave(DataModel) { DataModel dataModel ->
            dataModel.dataTypes.each { DataType dataType ->
                if (dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE && dataType.referenceClass) {
                    if (!dataModel.allDataClasses.contains(dataType.referenceClass)) {
                        dataType.referenceClass = dataModel.allDataClasses.find { DataClass dc ->
                            (dataType.referenceClass.id && dc.id && dc.id == dataType.referenceClass.id) ||
                            (dataType.referenceClass.label && dc.label && dc.label == dataType.referenceClass.label)
                        }
                    }
                }
            }
            dataTypesMapByModel.remove(dataModel)
            referenceTypesByModel.remove(dataModel)
        }

        // -----------------------------------------------------------------------
        // DataClass (enter) – wire the class into the model, propagate dataModel
        // and parentDataClass onto direct children, wire DataElements, and set
        // referenceTypes.  Depth-first traversal ensures a parent DataClass is
        // always entered before its children.
        // Mirrors: DataModel.setDataClassAssociations() (structural part)
        // -----------------------------------------------------------------------
        onEnter(DataClass) { DataClass dataClass ->
            DataModel dataModel = dataClass.dataModel
            if (!dataModel) return

            if (!dataModel.allDataClasses.contains(dataClass)) {
                dataModel.allDataClasses.add(dataClass)
            }

            // Propagate dataModel and parentDataClass onto direct child DataClasses.
            // Their onEnter(DataClass) will handle their own children in turn.
            dataClass.dataClasses?.each { DataClass child ->
                child.dataModel       = dataModel
                child.parentDataClass = dataClass
            }

            // Wire DataElement back-references so onEnter(DataElement) can see them.
            dataClass.dataElements?.each { DataElement de ->
                de.dataClass = dataClass
                de.dataModel = dataModel
            }

            // Identify which reference DataTypes point at this DataClass
            List<DataType> referenceTypes = referenceTypesByModel.get(dataModel)
            if (referenceTypes != null) {
                dataClass.referenceTypes = referenceTypes.findAll { DataType dt ->
                    dt.referenceClass?.id == dataClass.id
                } as List<DataType>
            }
        }

        // -----------------------------------------------------------------------
        // DataClass (leave) – resolve extendsDataClasses stubs against the real
        // DataClass instances now in allDataClasses.  Firing on leave (rather than
        // enter) means all descendants are already registered, matching the
        // depth-first resolution order of the original setDataClassAssociations().
        // Mirrors: DataModel.setDataClassAssociations() (extendsDataClasses part)
        // -----------------------------------------------------------------------
        onLeave(DataClass) { DataClass dataClass ->
            DataModel dataModel = dataClass.dataModel
            if (!dataModel || !dataClass.extendsDataClasses) return

            List<DataClass> resolvedExtends = []
            dataClass.extendsDataClasses.each { DataClass superClass ->
                DataClass found = dataModel.allDataClasses.find { DataClass dc ->
                    (superClass.id && dc.id && dc.id == superClass.id) ||
                    (superClass.label && dc.label && dc.label == superClass.label)
                }
                if (found) {
                    resolvedExtends.add(found)
                } else {
                    log.error(
                        'SetAssociationsVisitor onLeave(DataClass) failed to find a DataClass ' +
                        "for id=${superClass.id} or label=${superClass.label}")
                }
            }
            dataClass.extendsDataClasses = resolvedExtends
        }

        // -----------------------------------------------------------------------
        // DataElement (enter) – resolve the dataType stub and register the element
        // on the owning DataModel.  dataClass and dataModel are already set by
        // onEnter(DataClass).
        // Mirrors: DataModel.setDataClassAssociations() (DataElement part)
        // -----------------------------------------------------------------------
        onEnter(DataElement) { DataElement dataElement ->
            DataModel dataModel = dataElement.dataModel
            if (!dataModel) return

            // Resolve dataType reference
            if (dataElement.dataType) {
                Map<String, DataType> dataTypesMap = dataTypesMapByModel.get(dataModel)
                if (dataTypesMap) {
                    String key = dataElement.dataType.id?.toString() ?: dataElement.dataType.label
                    if (key) {
                        DataType found = dataTypesMap.get(key)
                        if (found == null) {
                            log.error(
                                'SetAssociationsVisitor onEnter(DataElement) failed to find a DataType ' +
                                "for id=${dataElement.dataType?.id} or label=${dataElement.dataType?.label}")
                        } else {
                            dataElement.dataType = found
                        }
                    }
                }
            }

            // Register the element on the DataModel (avoid duplicates)
            if (dataElement.id) {
                if (!dataModel.dataElements.any { DataElement e -> e.id == dataElement.id }) {
                    dataModel.dataElements.add(dataElement)
                }
            } else if (!dataModel.dataElements.contains(dataElement)) {
                dataModel.dataElements.add(dataElement)
            }
        }

        // -----------------------------------------------------------------------
        // DataType – set enumerationType on each EnumerationValue and register
        // the values with the owning DataModel.
        // Mirrors: DataType.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataType) { DataType dataType ->
            dataType.enumerationValues?.each { ev ->
                ev.enumerationType = dataType
                if (dataType.dataModel) {
                    dataType.dataModel.enumerationValues.add(ev)
                    ev.dataModel = dataType.dataModel
                }
            }
        }

        // -----------------------------------------------------------------------
        // Terminology – wire parent back-references on terms, relationship types,
        // and relationships; resolve relationship cross-references by id/code/label.
        //
        // Maps are built with explicit HashMap<Object,T> rather than collectEntries
        // so that @CompileStatic can verify the key/value types.
        //
        // Mirrors: Terminology.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Terminology) { Terminology terminology ->
            Map<Object, Term> termsMap = new HashMap<Object, Term>()
            terminology.terms.each { Term t -> termsMap.put(t.id ?: t.code, t) }

            Map<Object, TermRelationshipType> termRelTypesMap = new HashMap<Object, TermRelationshipType>()
            terminology.termRelationshipTypes.each { TermRelationshipType trt ->
                termRelTypesMap.put(trt.id ?: trt.label, trt)
            }

            terminology.terms?.each { Term term ->
                term.parent = terminology
            }
            terminology.termRelationshipTypes?.each { TermRelationshipType trt ->
                trt.parent = terminology
            }
            terminology.termRelationships?.each { TermRelationship rel ->
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

        // -----------------------------------------------------------------------
        // ClassificationScheme – propagate classificationScheme onto top-level
        // classifiers. Child classifiers are handled by onEnter(Classifier) below.
        // Mirrors: ClassificationScheme.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(ClassificationScheme) { ClassificationScheme cs ->
            cs.csClassifiers?.each { Classifier classifier ->
                classifier.classificationScheme = cs
            }
        }

        // -----------------------------------------------------------------------
        // Classifier – set parent and propagate classificationScheme / parentClassifier
        // onto immediate child classifiers (deeper nesting is handled recursively
        // by the traversal firing this handler for each classifier in the tree).
        // Mirrors: the classifier-loop body inside ClassificationScheme.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Classifier) { Classifier classifier ->
            classifier.parent = (classifier.parentClassifier ?: classifier.classificationScheme) as AdministeredItem
            classifier.childClassifiers?.each { Classifier child ->
                child.parentClassifier     = classifier
                child.classificationScheme = classifier.classificationScheme
                child.parent               = classifier
            }
        }

        // -----------------------------------------------------------------------
        // DataFlow – set dataFlow on DataClassComponents.
        // DataElementComponent back-references are set in onEnter(DataClassComponent)
        // so that neither the component list nor the element list is iterated twice.
        // Mirrors: DataFlow.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataFlow) { DataFlow dataFlow ->
            dataFlow.dataClassComponents?.each { DataClassComponent dcc ->
                dcc.dataFlow = dataFlow
            }
        }

        // -----------------------------------------------------------------------
        // DataClassComponent – set dataClassComponent back-reference on each
        // DataElementComponent.  Driven by the traversal, so each component is
        // visited exactly once.
        // Mirrors: the inner loop in DataFlow.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataClassComponent) { DataClassComponent dcc ->
            dcc.dataElementComponents?.each { DataElementComponent dec ->
                dec.dataClassComponent = dcc
            }
        }
    }
}

