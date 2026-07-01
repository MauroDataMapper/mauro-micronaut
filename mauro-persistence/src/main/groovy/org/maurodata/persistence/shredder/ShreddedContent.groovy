package org.maurodata.persistence.shredder

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
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.classifier.dto.ClassifierJoinDTO

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
    Set<ClassifierJoinDTO> classifierJoinDTOs = []
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

    List<UUID> getAllAdministeredItemIds() {
        List<UUID> administeredItemIds = [] as List<UUID>
        administeredItemIds.addAll(folders.values().flatten()*.id as List<UUID>)
        administeredItemIds.addAll(classificationSchemes*.id as List<UUID>)
        administeredItemIds.addAll(classifiers*.id as List<UUID>)
        administeredItemIds.addAll(dataModels*.id as List<UUID>)
        administeredItemIds.addAll(dataClasses.values().flatten()*.id as List<UUID>)
        administeredItemIds.addAll(dataTypes*.id as List<UUID>)
        administeredItemIds.addAll(dataElements*.id as List<UUID>)
        administeredItemIds.addAll(enumerationValues*.id as List<UUID>)
        administeredItemIds.addAll(terminologies*.id as List<UUID>)
        administeredItemIds.addAll(terms*.id as List<UUID>)
        administeredItemIds.addAll(termRelationshipTypes*.id as List<UUID>)
        administeredItemIds.addAll(termRelationships*.id as List<UUID>)
        administeredItemIds.addAll(codeSets*.id as List<UUID>)
        administeredItemIds.addAll(dataFlows*.id as List<UUID>)
        administeredItemIds.addAll(dataClassComponents*.id as List<UUID>)
        administeredItemIds.addAll(dataElementComponents*.id as List<UUID>)
        return administeredItemIds
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

}
