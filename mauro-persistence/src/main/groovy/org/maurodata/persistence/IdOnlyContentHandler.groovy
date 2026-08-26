package org.maurodata.persistence

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Item
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataFlowCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
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
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.folder.FolderRepository

import javax.sql.DataSource

@Slf4j
@CompileStatic
class IdOnlyContentHandler {

    @Inject DataSource dataSource

    @Inject FolderCacheableRepository folderCacheableRepository
    @Inject FolderRepository folderRepository
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

    @Inject MetadataRepository metadataRepository

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

    int batchSize = 10000

    Set<UUID> allItems = []

    Map<String, List<UUID>> parentDomains = [:]

    Map<String, List<List<UUID>>> recursiveParentDomains = [:]

    boolean deleteContent() {

        inBatches(parentDomains["Rule"].sort(), batchSize) {List<UUID> batch ->
            ruleRepresentationCacheableRepository.deleteAllByRuleIdIn(batch)
        }
        inBatches(parentDomains["SummaryMetadata"].sort(), batchSize) {List<UUID> batch ->
            summaryMetadataReportCacheableRepository.deleteAllBySummaryMetadataIdIn(batch)
        }

        inBatches(allItems.sort(), batchSize) {List<UUID> batch ->
            metadataRepository.deleteByMultiFacetAwareItemIdIn(batch)
            versionLinkCacheableRepository.deleteByMultiFacetAwareItemIdIn(batch)
            summaryMetadataCacheableRepository.deleteByMultiFacetAwareItemIdIn(batch)
            semanticLinkRepository.deleteByMultiFacetAwareItemIdIn(batch)
            ruleRepository.deleteByMultiFacetAwareItemIdIn(batch)
            referenceFileRepository.deleteByMultiFacetAwareItemIdIn(batch)
            editCacheableRepository.deleteByMultiFacetAwareItemIdIn(batch)
        }
        if(recursiveParentDomains['Annotation'] && recursiveParentDomains['Annotation'].size() > 0) {
            recursiveParentDomains['Annotation'].reverse().each {levelItems ->
                inBatches(levelItems.sort(), batchSize) {List<UUID> batch ->
                    annotationRepository.deleteAllByIdIn(batch)
                }
            }
        }

        inBatches(parentDomains["DataElementComponent"], batchSize) {List<UUID> batch ->
            // TODO: Do this in batch
            batch.each {dataElementComponent ->
                dataElementComponentCacheableRepository.removeSourceDataElements(dataElementComponent)
                dataElementComponentCacheableRepository.removeTargetDataElements(dataElementComponent)
            }
        }
        inBatches(parentDomains["DataElementComponent"]?.sort(), batchSize) {List<UUID> batch ->
            dataElementComponentCacheableRepository.deleteAllByIdIn(batch)
        }

        inBatches(parentDomains["DataClassComponent"], batchSize) {List<UUID> batch ->
            batch.each {dataClassComponent ->
                dataClassComponentCacheableRepository.removeSourceDataClasses(dataClassComponent)
                dataClassComponentCacheableRepository.removeTargetDataClasses(dataClassComponent)
            }
        }
        inBatches(parentDomains["DataClassComponent"]?.sort(), batchSize) {List<UUID> batch ->
            dataClassComponentCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["DataFlow"]?.sort(), batchSize) {List<UUID> batch ->
            dataFlowCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["EnumerationValue"]?.sort(), batchSize) {List<UUID> batch ->
            enumerationValueCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["DataElement"]?.sort(), batchSize) {List<UUID> batch ->
            dataElementCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["DataType"]?.sort(), batchSize) {List<UUID> batch ->
            dataTypeCacheableRepository.deleteAllByIdIn(batch)
        }
        if(recursiveParentDomains['DataClass'] && recursiveParentDomains['DataClass'].size() > 0) {
            recursiveParentDomains['DataClass'].reverse().each {levelItems ->
                inBatches(levelItems.sort(), batchSize) {List<UUID> batch ->
                    dataClassCacheableRepository.deleteExtensionRelationships(batch)
                    dataClassCacheableRepository.deleteAllByIdIn(batch)
                }
            }
        }

        inBatches(parentDomains["DataModel"]?.sort(), batchSize) {List<UUID> batch ->
            dataModelCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["CodeSet"]?.sort(), batchSize) {List<UUID> batch ->
            codeSetCacheableRepository.removeAllAssociations(batch)
        }
        inBatches(parentDomains["CodeSet"]?.sort(), batchSize) {List<UUID> batch ->
            codeSetCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["TermRelationship"]?.sort(), batchSize) {List<UUID> batch ->
            termRelationshipCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["TermRelationshipType"]?.sort(), batchSize) {List<UUID> batch ->
            termRelationshipTypeCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["Term"]?.sort(), batchSize) {List<UUID> batch ->
            termCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["Terminology"]?.sort(), batchSize) {List<UUID> batch ->
            terminologyCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["Classifier"]?.sort(), batchSize) {List<UUID> batch ->
            classifierCacheableRepository.deleteAllJoinAdministeredItemToClassifierIds(batch)
            classifierCacheableRepository.deleteAllByIdIn(batch)
        }
        inBatches(parentDomains["ClassificationScheme"]?.sort(), batchSize) {List<UUID> batch ->
            classificationSchemeCacheableRepository.deleteAllByIdIn(batch)
        }
        if(recursiveParentDomains['Folder'] && recursiveParentDomains['Folder'].size() > 0) {
            recursiveParentDomains['Folder'].reverse().each {levelItems ->
                inBatches(levelItems.sort(), batchSize) {List<UUID> batch ->
                    folderCacheableRepository.deleteAllByIdIn(batch)
                }
            }
        }
        true

    }


    private static <T extends Item> List<T> sortById(List<T> items) {
        return items.sort {it.id}
    }

    private static <T extends Item> Set<T> sortById(Set<T> items) {
        return items.sort {it.id} as Set<T>
    }

    private static <T> void inBatches(final List<T> items, final int batchSize, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) {
            return
        }
        final int itemsCount = items.size()
        if (itemsCount <= batchSize) {
            saver.call(items)
            return
        }
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<T> batch = items.subList(i, end)
            saver.call(batch)
        }
    }

    private static <T> void inBatches(final Set<UUID> ids, final int batchSize, @DelegatesTo(List) Closure saver) {
        if (ids == null || ids.isEmpty()) {
            return
        }
        final List<UUID> listIds = ids as List<UUID>
        final int itemsCount = listIds.size()
        if (itemsCount <= batchSize) {
            saver.call(listIds)
            return
        }
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<UUID> batch = listIds.subList(i, end)
            saver.call(batch)
        }
    }

    private static <I, T> List<T> inBatchesRead(final List<I> inputs, final int batchSize, @DelegatesTo(List) Closure reader) {
        if (inputs == null || inputs.isEmpty()) {
            return []
        }
        final int itemsCount = inputs.size()
        if (itemsCount <= batchSize) {
            return reader.call(inputs) as List<T>
        }
        final List<T> results = []
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<I> batch = inputs.subList(i, end)
            final List<T> page = reader.call(batch) as List<T>
            results.addAll(page)
        }
        return results
    }

    private static List<UUID> inBatchesReadSet(final List<UUID> inputs, final int batchSize, @DelegatesTo(List) Closure reader) {
        if (inputs == null || inputs.isEmpty()) {
            return [] as List<UUID>
        }
        final int itemsCount = inputs.size()
        if (itemsCount <= batchSize) {
            return reader.call(inputs) as List<UUID>
        }
        final List<UUID> results = []
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<UUID> batch = inputs.subList(i, end)
            final List<UUID> page = reader.call(batch) as List<UUID>
            results.addAll(page)
        }
        return results
    }

    boolean deleteContent(Folder folder) {
        recursiveParentDomains['Folder'] = [[folder.id]]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(ClassificationScheme classificationScheme) {
        parentDomains['ClassificationScheme'] = [classificationScheme.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataModel dataModel) {
        parentDomains['DataModel'] = [dataModel.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(Terminology terminology) {
        parentDomains['Terminology'] = [terminology.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(CodeSet codeSet) {
        parentDomains['CodeSet'] = [codeSet.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataClass dataClass) {
        recursiveParentDomains['DataClass'] = [[dataClass.id]]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(Term term) {
        parentDomains['Term'] = [term.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(TermRelationship termRelationship) {
        parentDomains['TermRelationship'] = [termRelationship.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataType dataType) {
        parentDomains['DataType'] = [dataType.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(EnumerationValue enumerationValue) {
        parentDomains['EnumerationValue'] = [enumerationValue.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataElement dataElement) {
        parentDomains['DataElement'] = [dataElement.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataFlow dataFlow) {
        parentDomains['DataFlow'] = [dataFlow.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataClassComponent dataClassComponent) {
        parentDomains['DataClassComponent'] = [dataClassComponent.id]
        loadContent()
        return deleteContent()
    }

    boolean deleteContent(DataElementComponent dataElementComponent) {
        parentDomains['DataElementComponent'] = [dataElementComponent.id]
        loadContent()
        return deleteContent()
    }

    void loadContent() {
        List<UUID> allFolders = []
        if (recursiveParentDomains['Folder'] && recursiveParentDomains['Folder'].size() > 0) {
            int depth = 1
            List<UUID> foundFolders = recursiveParentDomains['Folder'][0]
            allFolders.addAll(foundFolders)
            do {
                List<UUID> retrievedFolders = inBatchesRead(foundFolders as List<UUID>, batchSize) {List<UUID> batch ->
                    folderCacheableRepository.readAllIdByFolderIdIn(batch)
                }
                foundFolders = retrievedFolders
                if (foundFolders) {
                    recursiveParentDomains['Folder'][depth] = retrievedFolders
                }
                depth++
                allFolders.addAll(foundFolders)
            } while (foundFolders.size() > 0)
        }
        allItems.addAll(allFolders)

        if (allFolders) {
            parentDomains['ClassificationScheme'] = inBatchesReadSet(allFolders, batchSize) {List<UUID> batch ->
                classificationSchemeCacheableRepository.readAllIdByFolderIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['ClassificationScheme']?:[])

        if (parentDomains['ClassificationScheme']) {
            parentDomains['Classifier'] = inBatchesReadSet(parentDomains['ClassificationScheme'], batchSize) {List<UUID> batch ->
                classifierCacheableRepository.readAllIdByClassificationSchemeIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['Classifier']?:[])

        if (allFolders) {
            parentDomains['Terminology'] = inBatchesReadSet(allFolders, batchSize) {List<UUID> batch ->
                terminologyCacheableRepository.readAllIdByFolderIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['Terminology']?:[])

        if (parentDomains['Terminology']) {
            parentDomains['Term'] = inBatchesReadSet(parentDomains['Terminology'], batchSize) {List<UUID> batch ->
                termCacheableRepository.readAllIdByTerminologyIdIn(batch)
            }
            allItems.addAll(parentDomains['Term']?:[])

            parentDomains['TermRelationshipType'] = inBatchesReadSet(parentDomains['Terminology'], batchSize) {List<UUID> batch ->
                termRelationshipTypeCacheableRepository.readAllIdByTerminologyIdIn(batch)
            }
            allItems.addAll(parentDomains['TermRelationshipType']?:[])

            parentDomains['TermRelationship'] = inBatchesReadSet(parentDomains['Terminology'], batchSize) {List<UUID> batch ->
                termRelationshipCacheableRepository.readAllIdByTerminologyIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['TermRelationship']?:[])

        if (allFolders) {
            parentDomains['CodeSet'] = inBatchesReadSet(allFolders, batchSize) {List<UUID> batch ->
                codeSetCacheableRepository.readAllIdByFolderIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['CodeSet']?:[])

        if (allFolders) {
            parentDomains['DataModel'] = inBatchesReadSet(allFolders, batchSize) {List<UUID> batch ->
                dataModelCacheableRepository.readAllIdByFolderIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['DataModel']?:[])

        final List<UUID> allDataClasses = []
        if (parentDomains['DataModel']) {
            recursiveParentDomains['DataClass'] = [inBatchesReadSet(parentDomains['DataModel'], batchSize) {List<UUID> batch ->
                dataClassCacheableRepository.readAllIdByDataModelIdInAndParentDataClassIsNull(batch)
            }]
        }
        if (recursiveParentDomains['DataClass'] && recursiveParentDomains['DataClass'].size() > 0) {
            allDataClasses.addAll(recursiveParentDomains['DataClass'][0])
        }
        if (allDataClasses) {
            int depth = 1
            List<UUID> foundClasses = allDataClasses
            while (foundClasses.size() > 0) {
                List<UUID> retrievedDataClassIds = inBatchesRead(foundClasses, batchSize) {List<UUID> batch ->
                    dataClassCacheableRepository.readAllIdByParentDataClassIdIn(batch)
                }
                foundClasses = retrievedDataClassIds
                if (foundClasses) {
                    recursiveParentDomains['DataClass'][depth] = retrievedDataClassIds
                }
                allDataClasses.addAll(foundClasses)
                depth++
            }
        }
        allItems.addAll(allDataClasses)

        if (parentDomains['DataModel']) {
            parentDomains['DataType'] = inBatchesReadSet(parentDomains['DataModel'], batchSize) {List<UUID> batch ->
                dataTypeCacheableRepository.readAllIdByDataModelIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['DataType']?:[])

        if (parentDomains['DataType']) {
            parentDomains['EnumerationValue'] = inBatchesReadSet(parentDomains['DataType'], batchSize) {List<UUID> batch ->
                enumerationValueCacheableRepository.readAllIdByEnumerationTypeIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['EnumerationValue']?:[])

        if (allDataClasses) {
            parentDomains['DataElement'] = inBatchesReadSet(allDataClasses, batchSize) {List<UUID> batch ->
                dataElementCacheableRepository.readAllIdByDataClassIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['DataElement']?:[])

        if (parentDomains['DataModel']) {
            parentDomains['DataFlow'] = inBatchesReadSet(parentDomains['DataModel'], batchSize) {List<UUID> batch ->
                dataFlowCacheableRepository.readAllIdBySourceIdIn(batch)
            }
            parentDomains['DataFlow'].addAll(inBatchesReadSet(parentDomains['DataModel'], batchSize) {List<UUID> batch ->
                dataFlowCacheableRepository.readAllIdByTargetIdIn(batch)
            })
        }
        allItems.addAll(parentDomains['DataFlow']?:[])


        if (parentDomains['DataFlow']) {
            parentDomains['DataClassComponent'] = inBatchesReadSet(parentDomains['DataFlow'], batchSize) {List<UUID> batch ->
                dataClassComponentCacheableRepository.readAllIdByDataFlowIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['DataClassComponent']?:[])

        if (parentDomains['DataClassComponent']) {
            parentDomains['DataElementComponent'] = inBatchesReadSet(parentDomains['DataClassComponent'], batchSize) {List<UUID> batch ->
                dataElementComponentCacheableRepository.readAllIdByDataClassComponentIdIn(batch)
            }
        }
        allItems.addAll(parentDomains['DataElementComponent']?:[])

        // annotations

        if (allItems) {
            int depth = 1
            List<UUID> foundAnnotations = inBatchesReadSet(allItems as List<UUID>, batchSize) {List<UUID> batch ->
                annotationCacheableRepository.readAllIdByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull(batch)
            }
            recursiveParentDomains['Annotation'] = [foundAnnotations]
            if(foundAnnotations) {
                do {
                    List<UUID> retrievedAnnotations = inBatchesReadSet(foundAnnotations, batchSize) {List<UUID> batch ->
                        annotationCacheableRepository.readAllIdByParentAnnotationIdIn(batch)
                    }
                    foundAnnotations = retrievedAnnotations
                    if (foundAnnotations) {
                        recursiveParentDomains['Annotation'][depth] = retrievedAnnotations
                    }
                    depth++
                } while (foundAnnotations.size() > 0)
            }
        }

        parentDomains['Rule'] = inBatchesReadSet(allItems as List<UUID>, batchSize) {List batch ->
            ruleRepository.readAllIdByMultiFacetAwareItemIdIn(batch)
        }
        parentDomains['SummaryMetadata'] = inBatchesReadSet(allItems as List<UUID>, batchSize) {List<UUID> batch ->
            summaryMetadataCacheableRepository.readAllIdByMultiFacetAwareItemIdIn(batch)
        }
    }


}
