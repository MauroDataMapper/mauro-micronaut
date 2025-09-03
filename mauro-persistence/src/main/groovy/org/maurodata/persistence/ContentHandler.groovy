package org.maurodata.persistence

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.CodeSetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
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

    @Inject MetadataCacheableRepository metadataCacheableRepository

    @Inject MetadataRepository metadataRepository

    List<Folder> folders = []
    List<DataModel> dataModels = []
    List<DataClass> dataClasses = []
    List<DataType> dataTypes = []
    List<DataElement> dataElements = []
    List<EnumerationValue> enumerationValues = []
    List<Terminology> terminologies = []
    List<Term> terms = []
    List<TermRelationshipType> termRelationshipTypes = []
    List<TermRelationship> termRelationships = []
    List<CodeSet> codeSets = []

    List<Metadata> metadata = []


    void shred(Folder folder) {
        folders.add(folder)
        shredFacets(folder)
        folder.childFolders.each {shred(it)}
        folder.terminologies.each {shred(it)}
        folder.dataModels.each {shred(it)}
        folder.codeSets.each {shred(it)}
    }

    void shred(Terminology terminology) {
        terminologies.add(terminology)
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
    }

    void shred(TermRelationship termRelationship) {
        termRelationships.add(termRelationship)
    }

    void shred(CodeSet codeSet) {
        codeSets.add(codeSet)
    }

    void shred(DataModel dataModel) {
        dataModels.add(dataModel)
        dataModel.childDataClasses.each {shred(it)}
        dataModel.dataTypes.each {shred(it) }
    }

    void shred(DataClass dataClass) {
        dataClasses.add(dataClass)
        dataClass.dataClasses.each {shred(it)}
        dataClass.dataElements.each {shred(it)}
    }

    void shred(DataType dataType) {
        dataTypes.add(dataType)
        dataType.enumerationValues.each {shred(it)}
    }
    void shred(EnumerationValue enumerationValue) {
        enumerationValues.add(enumerationValue)
    }
    void shred(DataElement dataElement) {
        dataElements.add(dataElement)
    }

    void saveWithContent() {
        logSettings()
        folderCacheableRepository.saveAll(folders)
        terminologyCacheableRepository.saveAll(terminologies)
        termCacheableRepository.saveAll(terms)
        termRelationshipTypeCacheableRepository.saveAll(termRelationshipTypes)
        termRelationshipCacheableRepository.saveAll(termRelationships)
        codeSetCacheableRepository.saveAll(codeSets)

        metadata.each {it.multiFacetAwareItemId = it.multiFacetAwareItem.id }
/*
        for (int i = 0; i < metadata.size(); i += 5000) {
            int end = Math.min(i + 5000, metadata.size())
            log.error("Start save terminology")
            metadataCacheableRepository.saveAll(metadata.subList(i, end))
            log.error("End save terminology")
        }
*/
        Instant start = Instant.now()
        metadataRepository.saveAll(metadata)
        printTimeTaken(start)

    }

    void shredFacets(AdministeredItem item) {
        if(item.metadata) {
            metadata.addAll(item.metadata)
        }
    }

    void logSettings() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData()
            System.out.println("URL: " + meta.getURL())
            System.out.println("AutoCommit: " + conn.getAutoCommit())
        }
    }

    void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        System.err.println(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }

}
