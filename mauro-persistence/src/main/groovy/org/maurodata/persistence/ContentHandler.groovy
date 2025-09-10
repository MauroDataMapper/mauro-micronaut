package org.maurodata.persistence

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.CatalogueFile
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
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.EditCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.SummaryMetadataCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.VersionLinkCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.RuleRepresentationCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.SummaryMetadataReportCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.CodeSetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.facet.AnnotationRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.facet.ReferenceFileRepository
import org.maurodata.persistence.facet.RuleRepository
import org.maurodata.persistence.facet.SemanticLinkRepository
import org.maurodata.persistence.folder.FolderRepository

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@Slf4j
class ContentHandler {

    @Inject DataSource dataSource

    @Inject FolderCacheableRepository folderCacheableRepository
    @Inject FolderRepository folderRepository
    @Inject TerminologyCacheableRepository terminologyCacheableRepository
    @Inject TermCacheableRepository termCacheableRepository
    @Inject TermRelationshipTypeCacheableRepository termRelationshipTypeCacheableRepository
    @Inject TermRelationshipCacheableRepository termRelationshipCacheableRepository
    @Inject CodeSetCacheableRepository codeSetCacheableRepository
    @Inject DataModelCacheableRepository dataModelCacheableRepository
    @Inject DataClassCacheableRepository dataClassCacheableRepository
    @Inject DataTypeCacheableRepository dataTypeCacheableRepository
    @Inject DataElementCacheableRepository dataElementCacheableRepository
    @Inject EnumerationValueCacheableRepository enumerationValueCacheableRepository

    @Inject MetadataCacheableRepository metadataCacheableRepository

    @Inject MetadataRepository metadataRepository

    @Inject AnnotationRepository annotationRepository
    @Inject EditCacheableRepository editCacheableRepository
    @Inject ReferenceFileRepository referenceFileRepository
    @Inject FacetCacheableRepository.RuleCacheableRepository ruleRepository
    @Inject RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository
    @Inject SemanticLinkRepository semanticLinkRepository
    @Inject SummaryMetadataCacheableRepository summaryMetadataCacheableRepository
    @Inject SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository
    @Inject VersionLinkCacheableRepository versionLinkCacheableRepository

    Map<Integer, List<Folder>> folders = [:]
    List<DataModel> dataModels = []
    Map<Integer, List<DataClass>> dataClasses = [:]
    List<DataType> dataTypes = []
    List<DataElement> dataElements = []
    List<EnumerationValue> enumerationValues = []
    List<Terminology> terminologies = []
    List<Term> terms = []
    List<TermRelationshipType> termRelationshipTypes = []
    List<TermRelationship> termRelationships = []
    List<CodeSet> codeSets = []

    List<Metadata> metadata = []
    Map<Integer, List<Annotation>> annotations = [:]
    //List<CatalogueFile> catalogueFiles = []
    List<Edit> edits = []
    List<ReferenceFile> referenceFiles = []
    List<Rule> rules = []
    List<RuleRepresentation> ruleRepresentations = []
    List<SemanticLink> semanticLinks = []
    List<SummaryMetadata> summaryMetadata = []
    List<SummaryMetadataReport> summaryMetadataReports = []
    List<VersionLink> versionLinks = []

    void shred(Folder folder, Integer depth = 0) {
        if(folders[depth]) {
            folders[depth].add(folder)
        } else {
            folders[depth] = [folder] as Set
        }
        shredFacets(folder)
        folder.childFolders.each {shred(it, depth+1)}
        folder.terminologies.each {shred(it)}
        folder.dataModels.each {shred(it)}
        folder.codeSets.each {shred(it)}
    }

    void shred(Terminology terminology) {
        terminologies.add(terminology)
        shredFacets(terminology)
        terminology.terms.each {shred(it)}
        terminology.termRelationships.each {shred(it)}
        terminology.termRelationshipTypes.each {shred(it)}
    }

    void shred(Term term) {
        terms.add(term)
        shredFacets(term)
    }

    void shred(TermRelationshipType termRelationshipType) {
        termRelationshipTypes.add(termRelationshipType)
        shredFacets(termRelationshipType)
    }

    void shred(TermRelationship termRelationship) {
        termRelationships.add(termRelationship)
        shredFacets(termRelationship)
    }

    void shred(CodeSet codeSet) {
        codeSets.add(codeSet)
        shredFacets(codeSet)
    }

    void shred(DataModel dataModel) {
        dataModels.add(dataModel)
        shredFacets(dataModel)
        dataModel.childDataClasses.each {shred(it)}
        dataModel.dataTypes.each {shred(it) }
    }

    void shred(DataClass dataClass, Integer depth = 0) {
        if(dataClasses[depth]) {
            dataClasses[depth].add(dataClass)
        } else {
            dataClasses[depth] = [dataClass] as Set
        }
        shredFacets(dataClass)
        dataClass.dataClasses.each {shred(it, depth+1)}
        dataClass.dataElements.each {shred(it)}
    }

    void shred(DataType dataType) {
        dataTypes.add(dataType)
        shredFacets(dataType)
        dataType.enumerationValues.each {shred(it)}
    }
    void shred(EnumerationValue enumerationValue) {
        shredFacets(enumerationValue)
        enumerationValues.add(enumerationValue)
    }
    void shred(DataElement dataElement) {
        dataElements.add(dataElement)
        shredFacets(dataElement)
    }

    void saveWithContent() {
        folders.keySet().sort().each {depth ->
            folderCacheableRepository.saveAll(folders[depth])
        }
        terminologyCacheableRepository.saveAll(terminologies)
        termCacheableRepository.saveAll(terms)
        termRelationshipTypeCacheableRepository.saveAll(termRelationshipTypes)
        termRelationshipCacheableRepository.saveAll(termRelationships)
        codeSetCacheableRepository.saveAll(codeSets)
        dataModelCacheableRepository.saveAll (dataModels)
        dataClasses.keySet().sort().each {depth ->
            dataClassCacheableRepository.saveAll (dataClasses[depth])
        }
        dataClasses.values().flatten().each {DataClass dataClass ->
            dataClass.extendsDataClasses.each {superClass ->
                dataClassCacheableRepository.addDataClassExtensionRelationship(dataClass.id, superClass.id)
            }
        }
        dataTypeCacheableRepository.saveAll (dataTypes)
        dataElementCacheableRepository.saveAll (dataElements)
        enumerationValueCacheableRepository.saveAll (enumerationValues)

        metadata.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }

        annotations.keySet().sort().each {depth ->
            annotations[depth].each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
            annotationRepository.saveAll(annotations[depth])
        }

        edits.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
        editCacheableRepository.saveAll(edits)
        referenceFiles.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
        referenceFileRepository.saveAll(referenceFiles)
        rules.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
        ruleRepository.saveAll(rules)
        ruleRepresentationCacheableRepository.saveAll(ruleRepresentations)
        semanticLinks.each {
            it.multiFacetAwareItemId = it.multiFacetAwareItem.id
            // target?
        }
        semanticLinkRepository.saveAll(semanticLinks)
        summaryMetadata.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
        summaryMetadataCacheableRepository.saveAll(summaryMetadata)
        summaryMetadataReportCacheableRepository.saveAll(summaryMetadataReports)
        versionLinks.each {
            it.multiFacetAwareItemId = it.multiFacetAwareItem.id
//            it.targetModelId = it.targetModel.id
//            it.targetModelDomainType = it.targetModel.domainType
        }
        versionLinkCacheableRepository.saveAll(versionLinks)

/*
        for (int i = 0; i < metadata.size(); i += 5000) {
            int end = Math.min(i + 5000, metadata.size())
            log.error("Start save terminology")
            metadataCacheableRepository.saveAll(metadata.subList(i, end))
            log.error("End save terminology")
        }
*/
        Instant start = Instant.now()
        saveInBatches(metadata, 1000) { List<Metadata> batch ->
            metadataRepository.saveAll(batch)
        }
        printTimeTaken(start)

    }

    void shredFacets(AdministeredItem item) {
        if(item.metadata) {
            item.metadata.each {
                it.multiFacetAwareItem = item
                it.multiFacetAwareItemDomainType = item.domainType
            }
            metadata.addAll(item.metadata)
        }
    }

    void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        System.err.println(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }

    private static <T> void saveInBatches(List<T> items, int batchSize, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) return
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size())
            // Defensive copy so the batch does not retain a view of the full list
            List<T> batch = new ArrayList<>(items.subList(i, end))
            saver.call(batch)
        }
    }


}
