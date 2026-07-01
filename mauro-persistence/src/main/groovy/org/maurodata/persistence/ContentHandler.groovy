package org.maurodata.persistence

import org.maurodata.persistence.model.ItemRepository
import org.maurodata.shredder.ShreddedContent
import org.maurodata.persistence.terminology.dto.CodeSetTermDTO

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
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
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.Terminology
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
@CompileStatic
class ContentHandler {

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

    int batchSize = 10000





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

        inBatches(sortById(shreddedContent.dataElementComponents), batchSize) {List<DataElementComponent> batch ->
            // TODO: Do this in batch
            batch.each {dataElementComponent ->
                dataElementComponentCacheableRepository.removeSourceDataElements(dataElementComponent.id)
                dataElementComponentCacheableRepository.removeTargetDataElements(dataElementComponent.id)
            }
        }
        deleteAllByBatch(shreddedContent.dataElementComponents, dataElementComponentCacheableRepository)

        inBatches(sortById(shreddedContent.dataClassComponents), batchSize) {List<DataClassComponent> batch ->
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
        inBatches(sortById((List<DataClass>) shreddedContent.dataClasses.values().flatten()), batchSize) {List<DataClass> batch ->
            dataClassCacheableRepository.deleteExtensionRelationships(batch.collect {DataClass it -> it.id})
        }
        deleteHierarchyByBatch(shreddedContent.dataClasses, dataClassCacheableRepository)
        deleteAllByBatch(shreddedContent.dataModels, dataModelCacheableRepository)
        inBatches(sortById(shreddedContent.codeSets), batchSize) {List<CodeSet> batch ->
            codeSetCacheableRepository.removeAllAssociations(batch*.id)
        }
        deleteAllByBatch(shreddedContent.codeSets, codeSetCacheableRepository)
        deleteAllByBatch(shreddedContent.termRelationships, termRelationshipCacheableRepository)
        deleteAllByBatch(shreddedContent.termRelationshipTypes, termRelationshipTypeCacheableRepository)
        deleteAllByBatch(shreddedContent.terms, termCacheableRepository)
        deleteAllByBatch(shreddedContent.terminologies, terminologyCacheableRepository)

        inBatches(sortById(shreddedContent.classifiers), batchSize) {List<Classifier> batch ->
            classifierCacheableRepository.deleteAllJoinAdministeredItemToClassifierIds(batch*.id)
        }
        deleteAllByBatch(shreddedContent.classifiers, classifierCacheableRepository)
        deleteAllByBatch(shreddedContent.classificationSchemes, classificationSchemeCacheableRepository)
        deleteHierarchyByBatch(shreddedContent.folders, folderCacheableRepository)

        true
    }


    Folder loadWithContent(Folder folder) {
        folders[0] = [folder] as Set
        loadContent()
        return folders[0].first()
    }

    ClassificationScheme loadWithContent(ClassificationScheme classificationScheme) {
        classificationSchemes = [classificationScheme] as Set
        loadContent()
        return classificationSchemes.first()
    }

    DataModel loadWithContent(DataModel dataModel) {
        dataModels = [dataModel] as Set
        loadContent()
        return dataModels.first()
    }

    Terminology loadWithContent(Terminology terminology) {
        terminologies = [terminology] as Set
        loadContent()
        return terminologies.first()
    }

    CodeSet loadWithContent(CodeSet codeSet) {
        codeSets = [codeSet] as Set
        loadContent()
        return codeSets.first()
    }

    DataClass loadWithContent(DataClass dataClass) {
        dataClasses[0] = [dataClass] as Set
        loadContent()
        return dataClasses[0].first()
    }

    Term loadWithContent(Term term) {
        terms = [term] as Set
        loadContent()
        return terms.first()
    }

    TermRelationship loadWithContent(TermRelationship termRelationship) {
        termRelationships = [termRelationship] as Set
        loadContent()
        return termRelationships.first()
    }

    DataType loadWithContent(DataType dataType) {
        dataTypes = [dataType] as Set
        loadContent()
        return dataTypes.first()
    }

    EnumerationValue loadWithContent(EnumerationValue enumerationValue) {
        enumerationValues = [enumerationValue] as Set
        loadContent()
        return enumerationValues.first()
    }

    DataElement loadWithContent(DataElement dataElement) {
        dataElements = [dataElement] as Set
        loadContent()
        return dataElements.first()
    }

    DataFlow loadWithContent(DataFlow dataFlow) {
        dataFlows = [dataFlow] as Set
        loadContent()
        return dataFlows.first()
    }

    DataClassComponent loadWithContent(DataClassComponent dataClassComponent) {
        dataClassComponents = [dataClassComponent] as Set
        loadContent()
        return dataClassComponents.first()
    }

    DataElementComponent loadWithContent(DataElementComponent dataElementComponent) {
        dataElementComponents = [dataElementComponent] as Set
        loadContent()
        return dataElementComponents.first()
    }

    void loadContent(ShreddedContent shreddedContent) {
        if (shreddedContent.folders[0]) {
            int depth = 1
            Set<UUID> foundFolders = shreddedContent.folders[0]*.id as Set<UUID>
            do {
                Set<Folder> retrievedFolders = inBatchesRead(foundFolders as List<UUID>, batchSize) {List batch ->
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
        shreddedContent.allAdministeredItems.putAll(foldersValuesFlatten.collectEntries {[it.id, it]})
        if (foldersValuesFlatten) {
            shreddedContent.classificationSchemes = inBatchesReadSet(foldersValuesFlatten*.id, batchSize) {List<UUID> batch ->
                classificationSchemeCacheableRepository.readAllByFolderIdIn(batch)
            }
        }
        shreddedContent.allAdministeredItems.putAll(shreddedContent.classificationSchemes.collectEntries {[it.id, it]})

        if (classificationSchemes) {
            classifiers = inBatchesReadSet(classificationSchemes*.id, batchSize) {List<UUID> batch ->
                classifierCacheableRepository.readAllByClassificationSchemeIdIn(batch)
            }
        }
        allItems.putAll(classifiers.collectEntries {[it.id, it]})

        if (folders.values().flatten()) {
            terminologies = inBatchesReadSet(foldersValuesFlatten*.id, batchSize) {List batch ->
                terminologyCacheableRepository.readAllByFolderIdIn(batch)
            }
        }
        allItems.putAll(terminologies.collectEntries {[it.id, it]})

        if (terminologies) {
            terms = inBatchesReadSet(terminologies*.id, batchSize) {List<UUID> batch ->
                termCacheableRepository.readAllByTerminologyIdIn(batch)
            }
            // terms = termCacheableRepository.readAllByTerminologyIdIn(terminologies*.id) as Set<Term>
            allItems.putAll(terms.collectEntries {[it.id, it]})

            termRelationshipTypes = inBatchesReadSet(terminologies*.id, batchSize) {List batch ->
                termRelationshipTypeCacheableRepository.readAllByTerminologyIdIn(batch)
            }
            allItems.putAll(termRelationshipTypes.collectEntries {[it.id, it]})

            termRelationships = inBatchesReadSet(terminologies*.id, batchSize) {List batch ->
                termRelationshipCacheableRepository.readAllByTerminologyIdIn(batch)
            }
            allItems.putAll(termRelationships.collectEntries {[it.id, it]})
        }

        if (foldersValuesFlatten) {
            codeSets = inBatchesReadSet(foldersValuesFlatten*.id, batchSize) {List batch ->
                codeSetCacheableRepository.readAllByFolderIdIn(batch)
            }
        }
        allItems.putAll(codeSets.collectEntries {[it.id, it]})

        if (codeSets) {
            Map<UUID, CodeSet> codeSetMap = codeSets.collectEntries {[it.id, it]}
            Map<UUID, Term> termMap = terms.collectEntries {[it.id, it]}
            if (terms) {
                List<CodeSetTermDTO> listOfCodeSetTerm = inBatchesRead(codeSets*.id, batchSize) {List batch ->
                    codeSetCacheableRepository.getCodeSetTerms(batch)
                }
                listOfCodeSetTerm.each {codeSetTermDTO ->
                    codeSetMap[codeSetTermDTO.codeSetId].terms.add(termMap[codeSetTermDTO.termId])

                }
            } else {
                codeSets.each {codeSet ->
                    codeSet.terms = codeSetCacheableRepository.readTerms(codeSet.id)
                }
            }
        }

        if (foldersValuesFlatten) {
            dataModels = inBatchesReadSet(foldersValuesFlatten*.id, batchSize) {List batch ->
                dataModelCacheableRepository.readAllByFolderIdIn(batch)
            }
        }
        allItems.putAll(dataModels.collectEntries {[it.id, it]})
        if (dataModels) {
            dataClasses.put(0, inBatchesReadSet(dataModels*.id, batchSize) {List<UUID> batch ->
                dataClassCacheableRepository.readAllByDataModelIdInAndParentDataClassIsNull(batch)
            })
        }
        final Set<DataClass> dataClassesValuesFlatten
        if (dataClasses[0]) {
            int depth = 1
            Set<UUID> foundClasses = dataClasses[0]*.id as Set
            while (foundClasses.size() > 0) {
                List<DataClass> retrievedDataClasses = inBatchesRead(foundClasses as List<UUID>, batchSize) {List batch ->
                    dataClassCacheableRepository.readAllByParentDataClassIdIn(batch)
                }
                foundClasses = retrievedDataClasses*.id as Set
                if (foundClasses) {
                    dataClasses[depth] = retrievedDataClasses as Set
                }
                depth++
            }

            dataClassesValuesFlatten = (Set<DataClass>) dataClasses.values().flatten()
            allItems.putAll(dataClassesValuesFlatten.collectEntries {[it.id, it]})

            Map<UUID, DataClass> dataClassMap = dataClassesValuesFlatten.collectEntries {[it.id, it]}

            List<DataClassExtensionDTO> extensions = inBatchesRead(dataClassesValuesFlatten*.id, batchSize) {List batch ->
                dataClassCacheableRepository.getDataClassExtensionRelationships(batch)
            }
            extensions.each {
                dataClassMap[it.dataClassId].extendsDataClasses.add(dataClassMap[it.extendedDataClassId])
            }
        } else {
            dataClassesValuesFlatten = null
        }
        if (dataModels) {
            dataTypes = inBatchesReadSet(dataModels*.id, batchSize) {List batch ->
                dataTypeCacheableRepository.readAllByDataModelIdIn(batch)
            }
        }
        allItems.putAll(dataTypes.collectEntries {[it.id, it]})

        if (dataTypes) {
            enumerationValues = inBatchesReadSet(dataTypes*.id, batchSize) {List batch ->
                enumerationValueCacheableRepository.readAllByEnumerationTypeIdIn(batch)
            }
        }
        allItems.putAll(enumerationValues.collectEntries {[it.id, it]})

        if (dataClassesValuesFlatten) {
            dataElements = inBatchesReadSet(dataClassesValuesFlatten*.id, batchSize) {List batch ->
                dataElementCacheableRepository.readAllByDataClassIdIn(batch)
            }
        }
        allItems.putAll(dataElements.collectEntries {[it.id, it]})


        allItems.putAll(dataFlows.collectEntries {[it.id, it]})

        if (dataFlows) {
            dataClassComponents = inBatchesReadSet(dataFlows*.id, batchSize) {List batch ->
                dataClassComponentCacheableRepository.readAllByDataFlowIdIn(batch)
            }
        }
        allItems.putAll(dataClassComponents.collectEntries {[it.id, it]})
        dataClassComponents.each {dataClassComponent ->
            dataClassComponent.sourceDataClasses = dataClassComponentCacheableRepository.findAllSourceDataClasses(dataClassComponent.id)
            dataClassComponent.targetDataClasses = dataClassComponentCacheableRepository.findAllTargetDataClasses(dataClassComponent.id)
        }

        if (dataClassComponents) {
            dataElementComponents = inBatchesReadSet(dataClassComponents*.id, batchSize) {List batch ->
                dataElementComponentCacheableRepository.readAllByDataClassComponentIdIn(batch)
            }
        }
        allItems.putAll(dataElementComponents.collectEntries {[it.id, it]})
        dataElementComponents.each {dataElementComponent ->
            dataElementComponent.sourceDataElements = dataElementComponentCacheableRepository.getSourceDataElements(dataElementComponent.id)
            dataElementComponent.targetDataElements = dataElementComponentCacheableRepository.getTargetDataElements(dataElementComponent.id)
        }

        final Set<UUID> allItemsValuesId = allItems.values()*.id as Set<UUID>

        // annotations
        annotations.put(0, inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List<UUID> batch ->
            annotationCacheableRepository.readAllByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull(batch)
        })
        int depth = 1
        Set<UUID> foundAnnotations = annotations[0]*.id as Set<UUID>
        do {
            Set<Annotation> retrievedAnnotations = inBatchesReadSet(foundAnnotations as List<UUID>, batchSize) {List batch ->
                annotationCacheableRepository.readAllByParentAnnotationIdIn(batch)
            }
            foundAnnotations = retrievedAnnotations*.id as Set<UUID>
            if (foundAnnotations) {
                annotations[depth] = retrievedAnnotations
            }
            depth++
        } while (foundAnnotations.size() > 0)

        edits = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            editCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }

        classifierJoinDTOs = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            classifierCacheableRepository.readClassifiersByItemIds(batch)
        }

        classifiersForItems = inBatchesReadSet(classifierJoinDTOs*.classifierId as List<UUID>, batchSize) {List batch ->
            classifierCacheableRepository.readAllByIdIn(batch)
        }

        referenceFiles = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            referenceFileRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        rules = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            ruleRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        ruleRepresentations = inBatchesReadSet(rules*.id, batchSize) {List batch ->
            ruleRepresentationCacheableRepository.readAllByRuleIdIn(batch)
        }
        semanticLinks = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            semanticLinkRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        summaryMetadata = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            summaryMetadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }
        summaryMetadataReports = inBatchesReadSet(summaryMetadata*.id, batchSize) {List batch ->
            summaryMetadataReportCacheableRepository.readAllBySummaryMetadataIdIn(batch)
        }
        versionLinks = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            versionLinkCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }

        metadata = inBatchesReadSet(allItemsValuesId as List<UUID>, batchSize) {List batch ->
            metadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(batch)
        }

        reassemble()
    }

    void reassemble() {

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
        }

        dataClassComponents.each {dataClassComponent ->
            if(allItems[dataClassComponent.dataFlow.id]) {
                ((DataFlow) allItems[dataClassComponent.dataFlow.id]).dataClassComponents.add(dataClassComponent)
            }
        }

        dataElementComponents.each {dataElementComponent ->
            if(allItems[dataElementComponent.dataClassComponent.id]) {
                ((DataClassComponent) allItems[dataElementComponent.dataClassComponent.id]).dataElementComponents.add(dataElementComponent)
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
        classifierJoinDTOs.each { classifierJoinDTO ->
            allItems[classifierJoinDTO.catalogueItemId].classifiers.add(
                classifierMap[classifierJoinDTO.classifierId]
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

    static void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        log.info(String.format("Time taken: %sm %ss %sms",
                               timeTaken.toMinutesPart(),
                               timeTaken.toSecondsPart(),
                               timeTaken.toMillisPart()))
    }

    private static <T extends Item> Collection<T> ensureIdsAndSort(Collection<T> items) {
        items.each {item ->
            item.ensureId()
        }
        return sortById(items)
    }

    private static <T extends Item> Collection<T> sortById(Collection<T> items) {
        return items.sort {it.id}
    }

    private static <T> void  inBatches(final Collection<T> items, final int batchSize, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) {
            return
        }
        final List<T> listItems = items as List<T>
        final int itemsCount = listItems.size()
        if (itemsCount <= batchSize) {
            saver.call(listItems)
            return
        }
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<T> batch = listItems.subList(i, end)
            saver.call(batch)
        }
    }

    private static <I, T> Set<T> inBatchesReadSet(final List<I> inputs, final int batchSize, @DelegatesTo(List) Closure reader) {
        if (inputs == null || inputs.isEmpty()) {
            return [] as Set<T>
        }
        final int itemsCount = inputs.size()
        if (itemsCount <= batchSize) {
            return reader.call(inputs) as Set<T>
        }
        final List<T> results = []
        for (int i = 0; i < itemsCount; i += batchSize) {
            final int end = Math.min(i + batchSize, itemsCount)
            final List<I> batch = inputs.subList(i, end)
            final List<T> page = reader.call(batch) as List<T>
            results.addAll(page)
        }
        return results as Set<T>
    }


    private <T extends Item> void deleteAllByBatch(Collection<T> items, ItemRepository<T> itemRepository) {
        inBatches(sortById(items), batchSize) {List<T> batch ->
            itemRepository.deleteAll(batch)
        }
    }
    private <T extends Item> void deleteHierarchyByBatch(Map<Integer, Set<T>> items, ItemRepository<T> itemRepository) {
        items.keySet().sort().reverse().each {depth ->
            inBatches(sortById(items[depth]), batchSize) {List<T> batch ->
                itemRepository.deleteAll(batch)
            }
        }
    }

    private <T extends Item> void saveHierarchyByBatch(Map<Integer, Set<T>> items, ItemRepository<T> itemRepository) {
        items.keySet().sort().each {depth ->
            inBatches(ensureIdsAndSort(items[depth]), batchSize) {List<T> batches ->
                itemRepository.saveAll(batches)
            }
        }
    }

    private <T extends Item> void saveAllByBatch(Collection<T> items, ItemRepository<T> itemRepository) {
        inBatches(ensureIdsAndSort(items), batchSize) { List<T> batch ->
            itemRepository.saveAll(batch)
        }
    }


}
