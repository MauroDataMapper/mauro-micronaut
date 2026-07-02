package org.maurodata.persistence

import jakarta.inject.Singleton
import org.maurodata.persistence.model.ItemRepository
import org.maurodata.shredder.ShreddedContent
import org.maurodata.persistence.terminology.dto.CodeSetTermDTO

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Item
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataFlowCacheableRepository
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
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.CodeSetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.datamodel.dto.DataClassExtensionDTO

import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@Slf4j
@Singleton
@CompileStatic
class ContentHandler {

    final static int BATCH_SIZE = 10000

    @Inject DataSource dataSource

    @Inject FolderCacheableRepository folderCacheableRepository
    @Inject ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository
    @Inject AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository
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

    @Inject DataFlowCacheableRepository dataFlowCacheableRepository
    @Inject AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository
    @Inject AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository

    @Inject MetadataCacheableRepository metadataCacheableRepository

    @Inject FacetCacheableRepository.AnnotationCacheableRepository annotationRepository
    @Inject FacetCacheableRepository.AnnotationCacheableRepository annotationCacheableRepository
    @Inject EditCacheableRepository editCacheableRepository
    @Inject FacetCacheableRepository.ReferenceFileCacheableRepository referenceFileRepository
    @Inject FacetCacheableRepository.RuleCacheableRepository ruleRepository
    @Inject RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository
    @Inject FacetCacheableRepository.SemanticLinkCacheableRepository semanticLinkRepository
    @Inject SummaryMetadataCacheableRepository summaryMetadataCacheableRepository
    @Inject SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository
    @Inject VersionLinkCacheableRepository versionLinkCacheableRepository

    /**
     * Shred a folder hierarchy using the visitor pattern.
     * Traverses the entire domain tree and collects items into organized collections.
     */

    void saveWithContent(ShreddedContent shreddedContent) {
        Instant start = Instant.now()
        saveHierarchyByBatch(shreddedContent.folders, folderCacheableRepository)
        saveAllByBatch(shreddedContent.classificationSchemes, classificationSchemeCacheableRepository)
        saveAllByBatch(shreddedContent.classifiers, classifierCacheableRepository)
        saveAllByBatch(shreddedContent.terminologies, terminologyCacheableRepository)
        saveAllByBatch(shreddedContent.terms, termCacheableRepository)
        saveAllByBatch(shreddedContent.termRelationshipTypes, termRelationshipTypeCacheableRepository)
        saveAllByBatch(shreddedContent.termRelationships, termRelationshipCacheableRepository)
        saveAllByBatch(shreddedContent.codeSets, codeSetCacheableRepository)

        // TODO: Improve this by doing them in bulk
        // Actually maybe this happens automatically
        /*        codeSets.each {codeSet ->
                    codeSet.terms.each {term ->
                        codeSetCacheableRepository.addTerm(codeSet.id, term.id)
                    }
                }
        */

        saveAllByBatch(shreddedContent.dataModels, dataModelCacheableRepository)
        saveHierarchyByBatch(shreddedContent.dataClasses, dataClassCacheableRepository)
        // Note: didn't batch this one
        ((List<DataClass>) shreddedContent.dataClasses.values().flatten()).each {DataClass dataClass ->
            dataClass.extendsDataClasses.each {superClass ->
                dataClassCacheableRepository.addDataClassExtensionRelationship(dataClass.id, superClass.id)
            }
        }
        saveAllByBatch(shreddedContent.dataTypes, dataTypeCacheableRepository)
        saveAllByBatch(shreddedContent.dataElements, dataElementCacheableRepository)
        saveAllByBatch(shreddedContent.enumerationValues, enumerationValueCacheableRepository)
        saveAllByBatch(shreddedContent.dataFlows, dataFlowCacheableRepository)
        saveAllByBatch(shreddedContent.dataClassComponents, dataClassComponentCacheableRepository)
        saveAllByBatch(shreddedContent.dataElementComponents, dataElementComponentCacheableRepository)
        saveHierarchyByBatch(shreddedContent.annotations, annotationRepository)
        saveAllByBatch(shreddedContent.edits, editCacheableRepository)
        saveAllByBatch(shreddedContent.referenceFiles, referenceFileRepository)
        saveAllByBatch(shreddedContent.rules, ruleRepository)
        saveAllByBatch(shreddedContent.ruleRepresentations, ruleRepresentationCacheableRepository)
        saveAllByBatch(shreddedContent.semanticLinks, semanticLinkRepository)
        saveAllByBatch(shreddedContent.summaryMetadata, summaryMetadataCacheableRepository)
        saveAllByBatch(shreddedContent.summaryMetadataReports, summaryMetadataReportCacheableRepository)
        saveAllByBatch(shreddedContent.versionLinks, versionLinkCacheableRepository)
        saveAllByBatch(shreddedContent.metadata, metadataCacheableRepository)
        printTimeTaken(start)
    }


    boolean deleteWithContent(ShreddedContent shreddedContent) {

        deleteAllByBatch(shreddedContent.metadata, metadataCacheableRepository)
        deleteAllByBatch(shreddedContent.versionLinks, versionLinkCacheableRepository)
        deleteAllByBatch(shreddedContent.summaryMetadataReports, summaryMetadataReportCacheableRepository)
        deleteAllByBatch(shreddedContent.summaryMetadata, summaryMetadataCacheableRepository)
        deleteAllByBatch(shreddedContent.semanticLinks, semanticLinkRepository)
        deleteAllByBatch(shreddedContent.ruleRepresentations, ruleRepresentationCacheableRepository)
        deleteAllByBatch(shreddedContent.rules, ruleRepository)
        deleteAllByBatch(shreddedContent.referenceFiles, referenceFileRepository)
        deleteAllByBatch(shreddedContent.edits, editCacheableRepository)
        deleteHierarchyByBatch(shreddedContent.annotations, annotationRepository)

        inBatches(sortById(shreddedContent.dataElementComponents)) {List<DataElementComponent> batch ->
            // TODO: Do this in batch
            batch.each {dataElementComponent ->
                dataElementComponentCacheableRepository.removeSourceDataElements(dataElementComponent.id)
                dataElementComponentCacheableRepository.removeTargetDataElements(dataElementComponent.id)
            }
        }
        deleteAllByBatch(shreddedContent.dataElementComponents, dataElementComponentCacheableRepository)

        inBatches(sortById(shreddedContent.dataClassComponents)) {List<DataClassComponent> batch ->
            batch.each {dataClassComponent ->
                dataClassComponentCacheableRepository.removeSourceDataClasses(dataClassComponent.id)
                dataClassComponentCacheableRepository.removeTargetDataClasses(dataClassComponent.id)
            }
        }
        deleteAllByBatch(shreddedContent.dataClassComponents, dataClassComponentCacheableRepository)
        deleteAllByBatch(shreddedContent.dataFlows, dataFlowCacheableRepository)

        deleteAllByBatch(shreddedContent.enumerationValues, enumerationValueCacheableRepository)
        deleteAllByBatch(shreddedContent.dataElements, dataElementCacheableRepository)
        deleteAllByBatch(shreddedContent.dataTypes, dataTypeCacheableRepository)
        inBatches(sortById((List<DataClass>) shreddedContent.dataClasses.values().flatten())) {List<DataClass> batch ->
            dataClassCacheableRepository.deleteExtensionRelationships(batch.collect {DataClass it -> it.id})
        }
        deleteHierarchyByBatch(shreddedContent.dataClasses, dataClassCacheableRepository)
        deleteAllByBatch(shreddedContent.dataModels, dataModelCacheableRepository)
        inBatches(sortById(shreddedContent.codeSets)) {List<CodeSet> batch ->
            codeSetCacheableRepository.removeAllAssociations(batch*.id)
        }
        deleteAllByBatch(shreddedContent.codeSets, codeSetCacheableRepository)
        deleteAllByBatch(shreddedContent.termRelationships, termRelationshipCacheableRepository)
        deleteAllByBatch(shreddedContent.termRelationshipTypes, termRelationshipTypeCacheableRepository)
        deleteAllByBatch(shreddedContent.terms, termCacheableRepository)
        deleteAllByBatch(shreddedContent.terminologies, terminologyCacheableRepository)

        inBatches(sortById(shreddedContent.classifiers)) {List<Classifier> batch ->
            classifierCacheableRepository.deleteAllJoinAdministeredItemToClassifierIds(batch*.id)
        }
        deleteAllByBatch(shreddedContent.classifiers, classifierCacheableRepository)
        deleteAllByBatch(shreddedContent.classificationSchemes, classificationSchemeCacheableRepository)
        deleteHierarchyByBatch(shreddedContent.folders, folderCacheableRepository)

        true
    }



    void loadContent(ShreddedContent shreddedContent) {
        if (shreddedContent.folders[0]) {
            int depth = 1
            Set<UUID> foundFolders = shreddedContent.folders[0]*.id as Set<UUID>
            do {
                Set<Folder> retrievedFolders = inBatchesReadSet(foundFolders as List<UUID>) {List batch ->
                    folderCacheableRepository.readAllByFolderIdIn(batch)
                }
                foundFolders = retrievedFolders*.id as Set
                if (foundFolders) {
                    shreddedContent.folders[depth] = retrievedFolders as Set
                }
                depth++
            } while (foundFolders.size() > 0)
        }
        final Set<Folder> foldersValuesFlatten = (Set<Folder>) shreddedContent.folders.values().flatten()

        if (foldersValuesFlatten) {
            shreddedContent.classificationSchemes = inBatchesReadSet(foldersValuesFlatten*.id) {List<UUID> batch ->
                classificationSchemeCacheableRepository.readAllByFolderIdIn(batch)
            }
        }

        if (shreddedContent.classificationSchemes) {
            shreddedContent.classifiers = inBatchesReadSet(shreddedContent.classificationSchemes*.id) {List<UUID> batch ->
                classifierCacheableRepository.readAllByClassificationSchemeIdIn(batch)
            }
        }

        if (foldersValuesFlatten) {
            shreddedContent.terminologies = inBatchesReadSet(foldersValuesFlatten*.id) {List batch ->
                terminologyCacheableRepository.readAllByFolderIdIn(batch)
            }
        }

        if (shreddedContent.terminologies) {
            shreddedContent.terms = inBatchesReadSet(shreddedContent.terminologies*.id) {List<UUID> batch ->
                termCacheableRepository.readAllByTerminologyIdIn(batch)
            }

            shreddedContent.termRelationshipTypes = inBatchesReadSet(shreddedContent.terminologies*.id) {List batch ->
                termRelationshipTypeCacheableRepository.readAllByTerminologyIdIn(batch)
            }

            shreddedContent.termRelationships = inBatchesReadSet(shreddedContent.terminologies*.id) {List batch ->
                termRelationshipCacheableRepository.readAllByTerminologyIdIn(batch)
            }
        }

        if (foldersValuesFlatten) {
            shreddedContent.codeSets = inBatchesReadSet(foldersValuesFlatten*.id) {List batch ->
                codeSetCacheableRepository.readAllByFolderIdIn(batch)
            }
        }

        if (shreddedContent.codeSets) {
            Map<UUID, CodeSet> codeSetMap = shreddedContent.codeSets.collectEntries {[it.id, it]}
            Map<UUID, Term> termMap = shreddedContent.terms.collectEntries {[it.id, it]}
            if (shreddedContent.terms) {
                Set<CodeSetTermDTO> listOfCodeSetTerm = inBatchesReadSet(shreddedContent.codeSets*.id) {List batch ->
                    codeSetCacheableRepository.getCodeSetTerms(batch)
                }
                listOfCodeSetTerm.each {codeSetTermDTO ->
                    codeSetMap[codeSetTermDTO.codeSetId].terms.add(termMap[codeSetTermDTO.termId])

                }
            } else {
                shreddedContent.codeSets.each {codeSet ->
                    codeSet.terms = codeSetCacheableRepository.readTerms(codeSet.id)
                }
            }
        }

        if (foldersValuesFlatten) {
            shreddedContent.dataModels = inBatchesReadSet(foldersValuesFlatten*.id) {List batch ->
                dataModelCacheableRepository.readAllByFolderIdIn(batch)
            }
        }

        if (shreddedContent.dataModels) {
            shreddedContent.dataClasses.put(0, inBatchesReadSet(shreddedContent.dataModels*.id) {List<UUID> batch ->
                dataClassCacheableRepository.readAllByDataModelIdInAndParentDataClassIsNull(batch)
            })
        }
        final Set<DataClass> dataClassesValuesFlatten
        if (shreddedContent.dataClasses[0]) {
            int depth = 1
            Set<UUID> foundClasses = shreddedContent.dataClasses[0]*.id as Set
            while (foundClasses.size() > 0) {
                Set<DataClass> retrievedDataClasses = inBatchesReadSet(foundClasses as List<UUID>) {List batch ->
                    dataClassCacheableRepository.readAllByParentDataClassIdIn(batch)
                }
                foundClasses = retrievedDataClasses*.id as Set
                if (foundClasses) {
                    shreddedContent.dataClasses[depth] = retrievedDataClasses as Set
                }
                depth++
            }

            dataClassesValuesFlatten = (Set<DataClass>) shreddedContent.dataClasses.values().flatten()

            Map<UUID, DataClass> dataClassMap = dataClassesValuesFlatten.collectEntries {[it.id, it]}

            Set<DataClassExtensionDTO> extensions = inBatchesReadSet(dataClassesValuesFlatten*.id) {List batch ->
                dataClassCacheableRepository.getDataClassExtensionRelationships(batch)
            }
            extensions.each {
                dataClassMap[it.dataClassId].extendsDataClasses.add(dataClassMap[it.extendedDataClassId])
            }
        } else {
            dataClassesValuesFlatten = null
        }
        if (shreddedContent.dataModels) {
            shreddedContent.dataTypes = inBatchesReadSet(shreddedContent.dataModels*.id) {List batch ->
                dataTypeCacheableRepository.readAllByDataModelIdIn(batch)
            }
        }

        if (shreddedContent.dataTypes) {
            shreddedContent.enumerationValues = inBatchesReadSet(shreddedContent.dataTypes*.id) {List batch ->
                enumerationValueCacheableRepository.readAllByEnumerationTypeIdIn(batch)
            }
        }

        if (dataClassesValuesFlatten) {
            shreddedContent.dataElements = inBatchesReadSet(dataClassesValuesFlatten*.id) {List batch ->
                dataElementCacheableRepository.readAllByDataClassIdIn(batch)
            }
        }

        if(shreddedContent.dataModels) {
            shreddedContent.dataFlows = inBatchesReadSet(shreddedContent.dataModels*.id) {List batch ->
                dataFlowCacheableRepository.readAllBySourceIdIn(batch)
            }
        }

        if (shreddedContent.dataFlows) {
            shreddedContent.dataClassComponents = inBatchesReadSet(shreddedContent.dataFlows*.id) {List batch ->
                dataClassComponentCacheableRepository.readAllByDataFlowIdIn(batch)
            }
        }

        shreddedContent.dataClassComponents.each {dataClassComponent ->
            dataClassComponent.sourceDataClasses = dataClassComponentCacheableRepository.findAllSourceDataClasses(dataClassComponent.id)
            dataClassComponent.targetDataClasses = dataClassComponentCacheableRepository.findAllTargetDataClasses(dataClassComponent.id)
        }

        if (shreddedContent.dataClassComponents) {
            shreddedContent.dataElementComponents = inBatchesReadSet(shreddedContent.dataClassComponents*.id) {List batch ->
                dataElementComponentCacheableRepository.readAllByDataClassComponentIdIn(batch)
            }
        }
        shreddedContent.dataElementComponents.each {dataElementComponent ->
            dataElementComponent.sourceDataElements = dataElementComponentCacheableRepository.getSourceDataElements(dataElementComponent.id)
            dataElementComponent.targetDataElements = dataElementComponentCacheableRepository.getTargetDataElements(dataElementComponent.id)
        }

        final List<UUID> allAdministeredItemIds = shreddedContent.getAllAdministeredItemIds()

        // annotations
        shreddedContent.annotations.put(0, inBatchesReadSet(allAdministeredItemIds as List<UUID>) {List<UUID> batch ->
            annotationCacheableRepository.readAllByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull(batch)
        })
        int depth = 1
        List<UUID> foundAnnotations = shreddedContent.annotations[0]*.id
        do {
            Set<Annotation> retrievedAnnotations = inBatchesReadSet(foundAnnotations as List<UUID>) {List batch ->
                annotationCacheableRepository.readAllByParentAnnotationIdIn(batch)
            }
            foundAnnotations = retrievedAnnotations*.id
            if (foundAnnotations) {
                shreddedContent.annotations[depth] = retrievedAnnotations
            }
            depth++
        } while (foundAnnotations.size() > 0)

        shreddedContent.edits = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            editCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }

        shreddedContent.classifierJoinDTOs = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            classifierCacheableRepository.readClassifiersByItemIds(batch)
        }

        shreddedContent.classifiersForItems = inBatchesReadSet(shreddedContent.classifierJoinDTOs*.classifierId) {List batch ->
            classifierCacheableRepository.readAllByIdIn(batch)
        }

        shreddedContent.referenceFiles = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            referenceFileRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        shreddedContent.rules = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            ruleRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        shreddedContent.ruleRepresentations = inBatchesReadSet(shreddedContent.rules*.id) {List batch ->
            ruleRepresentationCacheableRepository.readAllByRuleIdIn(batch)
        }
        shreddedContent.semanticLinks = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            semanticLinkRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        shreddedContent.summaryMetadata = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            summaryMetadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        shreddedContent.summaryMetadataReports = inBatchesReadSet(shreddedContent.summaryMetadata*.id) {List batch ->
            summaryMetadataReportCacheableRepository.readAllBySummaryMetadataIdIn(batch)
        }
        shreddedContent.versionLinks = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            versionLinkCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        shreddedContent.metadata = inBatchesReadSet(allAdministeredItemIds) {List batch ->
            metadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }

        shreddedContent.reassemble()
    }

    static void printTimeTaken(Instant start, String taskDetails = "") {
        Duration timeTaken = Duration.between(start, Instant.now())
        log.info(String.format("Time taken ($taskDetails): %sm %ss %sms",
                               timeTaken.toMinutesPart(),
                               timeTaken.toSecondsPart(),
                               timeTaken.toMillisPart()))
    }

    static private <T extends Item> Collection<T> ensureIdsAndSort(Collection<T> items) {
        items.each {item ->
            item.ensureId()
        }
        return sortById(items)
    }

    static private <T extends Item> Collection<T> sortById(Collection<T> items) {
        return items.sort {it.id}
    }

    static private <T> void  inBatches(final Collection<T> items, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) {
            return
        }
        final List<T> listItems = items as List<T>
        final int itemsCount = listItems.size()
        if (itemsCount <= BATCH_SIZE) {
            saver.call(listItems)
            return
        }
        for (int i = 0; i < itemsCount; i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, itemsCount)
            final List<T> batch = listItems.subList(i, end)
            saver.call(batch)
        }
    }

    static private <I, T> Set<T> inBatchesReadSet(final List<I> inputs, @DelegatesTo(List) Closure reader) {
        if (inputs == null || inputs.isEmpty()) {
            return [] as Set<T>
        }
        final int itemsCount = inputs.size()
        if (itemsCount <= BATCH_SIZE) {
            return reader.call(inputs) as Set<T>
        }
        final List<T> results = []
        for (int i = 0; i < itemsCount; i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, itemsCount)
            final List<I> batch = inputs.subList(i, end)
            final List<T> page = reader.call(batch) as List<T>
            results.addAll(page)
        }
        return results as Set<T>
    }


    static private <T extends Item> void deleteAllByBatch(Collection<T> items, ItemRepository<T> itemRepository) {
        inBatches(sortById(items)) {List<T> batch ->
            itemRepository.deleteAll(batch)
        }
    }
    static private <T extends Item> void deleteHierarchyByBatch(Map<Integer, Set<T>> items, ItemRepository<T> itemRepository) {
        items.keySet().sort().reverse().each {depth ->
            inBatches(sortById(items[depth])) {List<T> batch ->
                itemRepository.deleteAll(batch)
            }
        }
    }

    static private <T extends Item> void saveHierarchyByBatch(Map<Integer, Set<T>> items, ItemRepository<T> itemRepository) {
        items.keySet().sort().each {depth ->
            inBatches(ensureIdsAndSort(items[depth])) {List<T> batches ->
                itemRepository.saveAll(batches)
            }
        }
    }

    static private <T extends Item> void saveAllByBatch(Collection<T> items, ItemRepository<T> itemRepository) {
        inBatches(ensureIdsAndSort(items)) {List<T> batch ->
            itemRepository.saveAll(batch)
        }
    }


}
