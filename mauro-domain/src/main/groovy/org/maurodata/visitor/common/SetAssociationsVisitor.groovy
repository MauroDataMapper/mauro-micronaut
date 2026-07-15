package org.maurodata.visitor.common

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
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.Term
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
 *   <li>{@link AdministeredItem#setAssociations()} – sets {@code multiFacetAwareItem} on all facets
 *       and back-links on summary-metadata reports and rule representations.</li>
 *   <li>{@link org.maurodata.domain.model.Model#setAssociations()} – additionally sets
 *       {@code multiFacetAwareItem} on version links.</li>
 *   <li>{@link Folder#setAssociations()} – wires {@code parentFolder} / {@code folder} onto
 *       contained child folders and models.</li>
 *   <li>{@link DataModel#setAssociations()} – sets {@code parent}/{@code dataModel} on data types,
 *       resolves data-class hierarchy and data-element/data-type cross-references.</li>
 *   <li>{@link DataType#setAssociations()} – sets {@code enumerationType} on enumeration values
 *       and registers them on the owning data model.</li>
 *   <li>{@link Annotation#setAssociations()} – sets {@code parentAnnotation} on child annotations.</li>
 *   <li>{@link Terminology#setAssociations()} – wires term/relationship-type/relationship
 *       back-references and resolves relationship cross-references.</li>
 *   <li>{@link ClassificationScheme#setAssociations()} – wires {@code classificationScheme} and
 *       {@code parentClassifier} onto the classifier tree.</li>
 *   <li>{@link org.maurodata.domain.dataflow.DataFlow#setAssociations()} – sets {@code dataFlow}
 *       and {@code dataClassComponent} back-references on flow components.</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 *   new SetAssociationsVisitor().visitFolder(rootFolder)
 *   // or for a standalone model:
 *   new SetAssociationsVisitor().visitDataModel(dataModel)
 * </pre>
 */
@Slf4j
class SetAssociationsVisitor extends GenericDomainTraversalVisitor {

    SetAssociationsVisitor() {

        // -----------------------------------------------------------------------
        // AdministeredItem – set multiFacetAwareItem on every facet, and wire up
        // the back-references inside SummaryMetadata and Rule.
        // Mirrors: AdministeredItem.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(AdministeredItem) { AdministeredItem item ->
            List<Facet> facets = []
            [item.edits, item.metadata, item.summaryMetadata, item.rules,
             item.annotations, item.referenceFiles, item.semanticLinks].each { collection ->
                facets.addAll(collection ?: [])
            }
            facets.each { Facet facet ->
                facet.multiFacetAwareItem = item
            }
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
            folder.childFolders?.each { childFolder ->
                childFolder.parentFolder = folder
            }
            folder.dataModels?.each { dataModel ->
                dataModel.folder = folder
            }
            folder.terminologies?.each { terminology ->
                terminology.folder = folder
            }
            folder.codeSets?.each { codeSet ->
                codeSet.folder = folder
            }
            folder.classificationSchemes?.each { cs ->
                cs.folder = folder
            }
        }

        // -----------------------------------------------------------------------
        // DataModel – wire parent/dataModel on data types, resolve the full
        // class hierarchy (parentDataClass, allDataClasses, extendsDataClasses),
        // and resolve dataElement -> dataType cross-references.
        // Mirrors: DataModel.setAssociations() / DataModel.setDataClassAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataModel) { DataModel dataModel ->
            // Build a lookup map: id (as String) → DataType, and label → DataType
            Map<String, DataType> dataTypesMap = [:]
            dataModel.dataTypes.each { DataType dt ->
                if (dt.id) dataTypesMap[dt.id.toString()] = dt
                if (dt.label) dataTypesMap[dt.label] = dt
            }
            List<DataType> referenceTypes = dataModel.dataTypes.findAll { DataType dt ->
                dt.isReferenceType()
            }

            // Set parent and dataModel back-references on every DataType
            dataModel.dataTypes.each { DataType dataType ->
                dataType.parent = dataModel
                dataType.dataModel = dataModel
            }

            // Recursively wire the DataClass tree and DataElements
            dataModel.dataClasses.each { DataClass dataClass ->
                wireDataClassAssociations(dataModel, dataClass, dataTypesMap, referenceTypes)
            }

            // Resolve referenceClass on any REFERENCE_TYPE DataTypes
            dataModel.dataTypes.each { DataType dataType ->
                if (dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE && dataType.referenceClass) {
                    if (!dataType.dataModel.allDataClasses.contains(dataType.referenceClass)) {
                        dataType.referenceClass = dataType.dataModel.allDataClasses.find { DataClass dc ->
                            (dataType.referenceClass.id && dc.id && dc.id == dataType.referenceClass.id) ||
                            (dataType.referenceClass.label && dc.label && dc.label == dataType.referenceClass.label)
                        }
                    }
                }
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
        // Mirrors: Terminology.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(Terminology) { Terminology terminology ->
            Map termsMap = terminology.terms.collectEntries { [(it.id ?: it.code): it] }
            Map termRelTypesMap = terminology.termRelationshipTypes.collectEntries { [(it.id ?: it.label): it] }

            terminology.terms?.each { term ->
                term.parent = terminology
            }
            terminology.termRelationshipTypes?.each { trt ->
                trt.parent = terminology
            }
            terminology.termRelationships?.each { rel ->
                rel.parent = terminology
                if (rel.relationshipType) {
                    rel.relationshipType = termRelTypesMap[rel.relationshipType.id ?: rel.relationshipType.label] as TermRelationshipType
                }
                if (rel.sourceTerm) {
                    rel.sourceTerm = termsMap[rel.sourceTerm.id ?: rel.sourceTerm.code] as Term
                }
                if (rel.targetTerm) {
                    rel.targetTerm = termsMap[rel.targetTerm.id ?: rel.targetTerm.code] as Term
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
            classifier.parent = classifier.parentClassifier ?: classifier.classificationScheme
            classifier.childClassifiers?.each { Classifier child ->
                child.parentClassifier = classifier
                child.classificationScheme = classifier.classificationScheme
                child.parent = classifier
            }
        }

        // -----------------------------------------------------------------------
        // DataFlow – set dataFlow on DataClassComponents and dataClassComponent on
        // DataElementComponents.
        // Mirrors: DataFlow.setAssociations()
        // -----------------------------------------------------------------------
        onEnter(DataFlow) { DataFlow dataFlow ->
            dataFlow.dataClassComponents?.each { DataClassComponent dcc ->
                dcc.dataFlow = dataFlow
                dcc.dataElementComponents?.each { DataElementComponent dec ->
                    dec.dataClassComponent = dcc
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helper: recursively wire a DataClass and its children into the DataModel.
    // Mirrors: DataModel.setDataClassAssociations()
    // ---------------------------------------------------------------------------
    private static void wireDataClassAssociations(DataModel dataModel,
                                                   DataClass dataClass,
                                                   Map<String, DataType> dataTypesMap,
                                                   List<DataType> referenceTypes) {
        dataClass.dataModel = dataModel
        if (!dataModel.allDataClasses.contains(dataClass)) {
            dataModel.allDataClasses.add(dataClass)
        }

        // Recursively wire child DataClasses first (depth-first, matching original behaviour)
        dataClass.dataClasses?.each { DataClass childDataClass ->
            wireDataClassAssociations(dataModel, childDataClass, dataTypesMap, referenceTypes)
            childDataClass.parentDataClass = dataClass
        }

        // Resolve extendsDataClasses stubs to real DataClass instances
        List<DataClass> resolvedExtends = []
        dataClass.extendsDataClasses?.each { DataClass superClass ->
            DataClass found = dataModel.allDataClasses.find { DataClass dc ->
                (superClass.id && dc.id && dc.id == superClass.id) ||
                (superClass.label && dc.label && dc.label == superClass.label)
            }
            if (found) {
                resolvedExtends.add(found)
            } else {
                log.error(
                    'SetAssociationsVisitor wireDataClassAssociations() failed to find a DataClass ' +
                    "for id=${superClass.id} or label=${superClass.label}")
            }
        }
        dataClass.extendsDataClasses = resolvedExtends

        // Wire DataElements: resolve dataType, set back-references, register on DataModel
        dataClass.dataElements?.each { DataElement dataElement ->
            dataElement.dataModel = dataModel
            dataElement.dataClass = dataClass

            String dataTypeKey = dataElement.dataType?.id?.toString() ?: dataElement.dataType?.label
            if (dataTypeKey) {
                DataType foundDataType = dataTypesMap[dataTypeKey]
                if (foundDataType == null) {
                    log.error(
                        'SetAssociationsVisitor wireDataClassAssociations() failed to find a DataType ' +
                        "for id=${dataElement.dataType?.id} or label=${dataElement.dataType?.label}")
                } else {
                    dataElement.dataType = foundDataType
                }
            }

            if (dataElement.id && !dataModel.dataElements*.id.contains(dataElement.id)) {
                dataModel.dataElements.add(dataElement)
            } else if (!dataElement.id && !dataModel.dataElements.contains(dataElement)) {
                dataModel.dataElements.add(dataElement)
            }
        }

        // Mark which reference DataTypes point at this DataClass
        dataClass.referenceTypes = referenceTypes.findAll { DataType dt ->
            dt.referenceClass?.id == dataClass.id
        } as List<DataType>
    }
}



