package org.maurodata.shredder

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
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

@Slf4j
class ShreddedContent {

    Map<Integer, Set<Folder>> folders = [:]
    Set<ClassificationScheme> classificationSchemes = []
    Set<Classifier> classifiers = []
    Set<DataModel> dataModels = []
    Map<Integer, Set<DataClass>> dataClasses = [:]
    Set<DataType> dataTypes = []
    Set<DataElement> dataElements = []
    Set<EnumerationValue> enumerationValues = []
    Set<Terminology> terminologies = []
    Set<Term> terms = []
    Set<TermRelationshipType> termRelationshipTypes = []
    Set<TermRelationship> termRelationships = []
    Set<CodeSet> codeSets = []
    Set<DataFlow> dataFlows = []
    Set<DataClassComponent> dataClassComponents = []
    Set<DataElementComponent> dataElementComponents = []

    Set<Metadata> metadata = []
    Map<UUID, Set<UUID>> classifierJoins = [:]
    Set<Classifier> classifiersForItems = []
    Map<Integer, Set<Annotation>> annotations = [:] as Map<Integer, Set<Annotation>>
    Set<Edit> edits = []
    Set<ReferenceFile> referenceFiles = []
    Set<Rule> rules = []
    Set<RuleRepresentation> ruleRepresentations = []
    Set<SemanticLink> semanticLinks = []
    Set<SummaryMetadata> summaryMetadata = []
    Set<SummaryMetadataReport> summaryMetadataReports = []
    Set<VersionLink> versionLinks = []

    List<AdministeredItem> getAllAdministeredItems() {
        List<AdministeredItem> administeredItems = []
        administeredItems.addAll(folders.values().flatten() as List<Folder>)
        administeredItems.addAll(classificationSchemes)
        administeredItems.addAll(classifiers)
        administeredItems.addAll(dataModels)
        administeredItems.addAll(dataClasses.values().flatten() as List<DataClass>)
        administeredItems.addAll(dataTypes)
        administeredItems.addAll(dataElements)
        administeredItems.addAll(enumerationValues)
        administeredItems.addAll(terminologies)
        administeredItems.addAll(terms)
        administeredItems.addAll(termRelationshipTypes)
        administeredItems.addAll(termRelationships)
        administeredItems.addAll(codeSets)
        administeredItems.addAll(dataFlows)
        administeredItems.addAll(dataClassComponents)
        administeredItems.addAll(dataElementComponents)
        return administeredItems
    }

    List<UUID> getAllAdministeredItemIds() {
        getAllAdministeredItems().collect { it.id }
    }

    void addFolderAtDepth(Folder folder, Integer depth) {
        if (folders[depth]) {
            folders[depth].add(folder)
        } else {
            folders[depth] = [folder] as Set
        }
    }

    void addDataClassAtDepth(DataClass dataClass, Integer depth) {
        if (dataClasses[depth]) {
            dataClasses[depth].add(dataClass)
        } else {
            dataClasses[depth] = [dataClass] as Set
        }
    }

    void addAnnotationAtDepth(Annotation annotation, Integer depth) {
        if (annotations[depth]) {
            annotations[depth].add(annotation)
        } else {
            annotations[depth] = [annotation] as Set
        }
    }

    ShreddedContent() {

    }

    ShreddedContent(AdministeredItem administeredItem) {
        if (administeredItem instanceof Folder) {
            addFolderAtDepth((Folder) administeredItem, 0)
        } else if (administeredItem instanceof ClassificationScheme) {
            classificationSchemes.add((ClassificationScheme) administeredItem)
        } else if (administeredItem instanceof Classifier) {
            classifiers.add((Classifier) administeredItem)
        } else if (administeredItem instanceof DataModel) {
            dataModels.add((DataModel) administeredItem)
        } else if (administeredItem instanceof DataClass) {
            addDataClassAtDepth((DataClass) administeredItem, 0)
        } else if (administeredItem instanceof DataType) {
            dataTypes.add((DataType) administeredItem)
        } else if (administeredItem instanceof DataElement) {
            dataElements.add((DataElement) administeredItem)
        } else if (administeredItem instanceof EnumerationValue) {
            enumerationValues.add((EnumerationValue) administeredItem)
        } else if (administeredItem instanceof Terminology) {
            terminologies.add((Terminology) administeredItem)
        } else if (administeredItem instanceof Term) {
            terms.add((Term) administeredItem)
        } else if (administeredItem instanceof TermRelationshipType) {
            termRelationshipTypes.add((TermRelationshipType) administeredItem)
        } else if (administeredItem instanceof TermRelationship) {
            termRelationships.add((TermRelationship) administeredItem)
        } else if (administeredItem instanceof CodeSet) {
            codeSets.add((CodeSet) administeredItem)
        } else if (administeredItem instanceof DataFlow) {
            dataFlows.add((DataFlow) administeredItem)
        } else if (administeredItem instanceof DataClassComponent) {
            dataClassComponents.add((DataClassComponent) administeredItem)
        } else if (administeredItem instanceof DataElementComponent) {
            dataElementComponents.add((DataElementComponent) administeredItem)
        }
    }

    void reassemble() {

        Map<UUID, AdministeredItem> allItems = getAllAdministeredItems().collectEntries({[it.id, it]})

        folders.keySet().sort().each {depth ->
            if(depth > 0) {
                folders[depth].each {folder ->
                    ((Folder) allItems[folder.parentFolder.id]).childFolders.add(folder)
                }
            }
        }
        classificationSchemes.each {classificationScheme ->
            if(allItems[classificationScheme.folder.id]) {
                ((Folder) allItems[classificationScheme.folder.id]).classificationSchemes.add(classificationScheme)
            }
        }

        classifiers.each {classifier ->
            ((ClassificationScheme) allItems[classifier.classificationScheme.id]).csClassifiers.add(classifier)
        }

        terminologies.each {terminology ->
            if(allItems[terminology.folder.id]) {
                ((Folder) allItems[terminology.folder.id]).terminologies.add(terminology)
            }
        }
        terms.each {term ->
            if(allItems[term.terminology.id]) {
                ((Terminology) allItems[term.terminology.id]).terms.add(term)
            }
        }
        termRelationshipTypes.each {termRelationshipType ->
            if(allItems[termRelationshipType.terminology.id]) {
                ((Terminology) allItems[termRelationshipType.terminology.id]).termRelationshipTypes.add(termRelationshipType)
            }
        }
        termRelationships.each {termRelationship ->
            if(allItems[termRelationship.terminology.id]) {
                ((Terminology) allItems[termRelationship.terminology.id]).termRelationships.add(termRelationship)
            }
        }
        codeSets.each {codeSet ->
            if(allItems[codeSet.folder.id]) {
                ((Folder) allItems[codeSet.folder.id]).codeSets.add(codeSet)
            }
        }
        dataModels.each {dataModel ->
            if(allItems[dataModel.folder.id]) {
                ((Folder) allItems[dataModel.folder.id]).dataModels.add(dataModel)
            }
        }

        dataClasses.keySet().sort().each {depth ->
            dataClasses[depth].each {dataClass ->
                if(dataClass.parentDataClass && allItems[dataClass.parentDataClass.id] ) {
                    ((DataClass) allItems[dataClass.parentDataClass.id]).dataClasses.add(dataClass)
                } else if(allItems[dataClass.dataModel.id]) {
                    ((DataModel) allItems[dataClass.dataModel.id]).dataClasses.add(dataClass)
                }
            }
        }
        dataTypes.each {dataType ->
            if(allItems[dataType.dataModel.id]) {
                ((DataModel) allItems[dataType.dataModel.id]).dataTypes.add(dataType)
            }
        }
        enumerationValues.each {enumerationValue ->
            if(allItems[enumerationValue.enumerationType.id]) {
                ((DataType) allItems[enumerationValue.enumerationType.id]).enumerationValues.add(enumerationValue)
            }
        }

        dataElements.each {dataElement ->
            if(allItems[dataElement.dataClass.id]) {
                ((DataClass) allItems[dataElement.dataClass.id]).dataElements.add(dataElement)
            }
            if(allItems[dataElement.dataType.id]) {
                dataElement.dataType = ((DataType) allItems[dataElement.dataType.id])
            }
        }

        dataFlows.each {dataFlow ->
            if(allItems[dataFlow.source.id]) {
                dataFlow.source = ((DataModel) allItems[dataFlow.source.id])
            }
            if(allItems[dataFlow.target.id]) {
                dataFlow.target = ((DataModel) allItems[dataFlow.target.id])
            }
        }

        dataClassComponents.each {dataClassComponent ->
            if(allItems[dataClassComponent.dataFlow.id]) {
                ((DataFlow) allItems[dataClassComponent.dataFlow.id]).dataClassComponents.add(dataClassComponent)
            }
            dataClassComponent.sourceDataClasses.each {sourceDataClass ->
                if(allItems[sourceDataClass.id]) {
                    dataClassComponent.sourceDataClasses.add((DataClass) allItems[sourceDataClass.id])
                }
            }
            dataClassComponent.targetDataClasses.each {targetDataClass ->
                if(allItems[targetDataClass.id]) {
                    dataClassComponent.targetDataClasses.add((DataClass) allItems[targetDataClass.id])
                }
            }
        }

        dataElementComponents.each {dataElementComponent ->
            if(allItems[dataElementComponent.dataClassComponent.id]) {
                ((DataClassComponent) allItems[dataElementComponent.dataClassComponent.id]).dataElementComponents.add(dataElementComponent)
            }
            dataElementComponent.sourceDataElements.each {sourceDataElement ->
                if(allItems[sourceDataElement.id]) {
                    dataElementComponent.sourceDataElements.add((DataElement) allItems[sourceDataElement.id])
                }
            }
            dataElementComponent.targetDataElements.each {targetDataElement ->
                if (allItems[targetDataElement.id]) {
                    dataElementComponent.targetDataElements.add((DataElement) allItems[targetDataElement.id])
                }
            }
        }

        edits.each {edit ->
            if(allItems[edit.multiFacetAwareItemId]) {
                allItems[edit.multiFacetAwareItemId].edits.add(edit)
            }
        }

        Map<UUID, Classifier> classifierMap = classifiersForItems.collectEntries {
            [it.id, it]
        }
        classifierJoins.each { classifierJoin ->
            allItems[classifierJoin.key].classifiers.addAll(
                classifierJoin.value.collect { classifierMap[it]}
            )
        }

        annotations[0].each {annotation ->
            if(allItems[annotation.multiFacetAwareItemId]) {
                allItems[annotation.multiFacetAwareItemId].annotations.add(annotation)
            }
        }
        annotations.keySet().sort().each {depth ->
            if(depth > 0) {
                annotations[depth].each {annotation ->
                    if (annotation.parentAnnotationId) {
                        annotations[depth-1].find {it.id == annotation.parentAnnotationId}.childAnnotations.add(annotation)
                    } else {
                        log.error("'parentAnnotationId' not set on child Annotation: ${annotation.label}")
                    }
                }
            }
        }

        referenceFiles.each {referenceFile ->
            allItems[referenceFile.multiFacetAwareItemId].referenceFiles.add(referenceFile)
        }
        rules.each {rule ->
            allItems[rule.multiFacetAwareItemId].rules.add(rule)
        }

        Map<UUID, Rule> ruleMap = rules.collectEntries{ [it.id, it]}
        ruleRepresentations.each {ruleRepresentation ->
            ruleMap[ruleRepresentation.ruleId].ruleRepresentations.add(ruleRepresentation)
        }

        semanticLinks.each {semanticLink ->
            allItems[semanticLink.multiFacetAwareItemId].semanticLinks.add(semanticLink)
            if(allItems[semanticLink.targetMultiFacetAwareItemId]) {
                semanticLink.target = allItems[semanticLink.targetMultiFacetAwareItemId]
            }
        }

        summaryMetadata.each {summaryMetadata ->
            allItems[summaryMetadata.multiFacetAwareItemId].summaryMetadata.add(summaryMetadata)
        }

        Map<UUID, SummaryMetadata> summaryMetadataMap = summaryMetadata.collectEntries {[it.id, it]}
        summaryMetadataReports.each {summaryMetadataReport ->
            summaryMetadataMap[summaryMetadataReport.summaryMetadataId].summaryMetadataReports.add(summaryMetadataReport)
        }

        versionLinks.each {versionLink ->
            ((Model) allItems[versionLink.multiFacetAwareItemId]).versionLinks.add(versionLink)
        }

        metadata.each {metadata ->
            allItems[metadata.multiFacetAwareItemId].metadata.add(metadata)
        }
    }

    void unsetIdentifiers() {
        allAdministeredItems.each {item ->
            item.id = null
        }
    }

}
