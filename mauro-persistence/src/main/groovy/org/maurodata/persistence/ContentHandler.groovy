package org.maurodata.persistence

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
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Facet
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
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
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
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.folder.FolderRepository

import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@Slf4j
@CompileStatic
class ContentHandler {

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

    int batchSize = 1000

    Map<UUID, AdministeredItem> allItems = [:]


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
    Map<Integer, Set<Annotation>> annotations = [:]
    Set<Edit> edits = []
    Set<ReferenceFile> referenceFiles = []
    Set<Rule> rules = []
    Set<RuleRepresentation> ruleRepresentations = []
    Set<SemanticLink> semanticLinks = []
    Set<SummaryMetadata> summaryMetadata = []
    Set<SummaryMetadataReport> summaryMetadataReports = []
    Set<VersionLink> versionLinks = []

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
        folder.classificationSchemes.each { shred(it)}
    }

    void shred(ClassificationScheme classificationScheme) {
        classificationSchemes.add(classificationScheme)
        shredFacets(classificationScheme)
        classificationScheme.csClassifiers.each {shred(it)}
    }

    void shred(Classifier classifier) {
        classifiers.add(classifier)
        shredFacets(classifier)
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
        dataModel.dataClasses.each {shred(it)}
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

    void shred(DataFlow dataFlow) {
        dataFlows.add(dataFlow)
        shredFacets(dataFlow)
        dataFlow.dataClassComponents.each { shred(it)}
    }

    void shred(DataClassComponent dataClassComponent) {
        dataClassComponents.add(dataClassComponent)
        shredFacets(dataClassComponent)
        dataClassComponent.dataElementComponents.each { shred(it)}
    }

    void shred(DataElementComponent dataElementComponent) {
        dataElementComponents.add(dataElementComponent)
        shredFacets(dataElementComponent)
    }

    void saveWithContent() {
        folders.keySet().sort().each {depth ->
            inBatches(folders[depth].findAll {!it.id}, batchSize) {List<Folder> batches ->
                folderCacheableRepository.saveAll(batches)
            }
        }
        inBatches(classificationSchemes.findAll {!it.id}, batchSize) {List<ClassificationScheme> batches ->
            classificationSchemeCacheableRepository.saveAll(batches)
        }
        inBatches(classifiers.findAll {!it.id}, batchSize) {List<Classifier> batches ->
            classifierCacheableRepository.saveAll(batches)
        }
        inBatches(terminologies.findAll {!it.id}, batchSize) {List<Terminology> batches ->
            terminologyCacheableRepository.saveAll(batches)
        }
        inBatches(terms.findAll {!it.id}, batchSize) {List<Term> batches ->
            termCacheableRepository.saveAll(batches)
        }
        inBatches(termRelationshipTypes.findAll {!it.id}, batchSize) {List<TermRelationshipType> batches ->
            termRelationshipTypeCacheableRepository.saveAll(batches)
        }
        inBatches(termRelationships.findAll {!it.id}, batchSize) {List<TermRelationship> batches ->
            termRelationshipCacheableRepository.saveAll(batches)
        }
        inBatches(codeSets.findAll {!it.id}, batchSize) {List<CodeSet> batches ->
            codeSetCacheableRepository.saveAll(batches)
        }

        // TODO: Improve this by doing them in bulk
        // Actually maybe this happens automatically
        /*        codeSets.each {codeSet ->
                    codeSet.terms.each {term ->
                        codeSetCacheableRepository.addTerm(codeSet.id, term.id)
                    }
                }
        */
        inBatches(dataModels.findAll {!it.id}, batchSize) {List<DataModel> batches ->
            dataModelCacheableRepository.saveAll(batches)
        }

        dataClasses.keySet().sort().each {depth ->
            inBatches(dataClasses[depth].findAll {!it.id}, batchSize) {List<DataClass> batches ->
                dataClassCacheableRepository.saveAll(batches)
            }
        }
        // Note: didn't batch this one
        ((List<DataClass>) dataClasses.values().flatten()).each {DataClass dataClass ->
            dataClass.extendsDataClasses.each {superClass ->
                dataClassCacheableRepository.addDataClassExtensionRelationship(dataClass.id, superClass.id)
            }
        }
        inBatches(dataTypes.findAll {!it.id}, batchSize) {List<DataType> batches ->
            dataTypeCacheableRepository.saveAll(batches)
        }
        inBatches(dataElements.findAll {!it.id}, batchSize) {List<DataElement> batches ->
            dataElementCacheableRepository.saveAll(batches)
        }
        inBatches(enumerationValues.findAll {!it.id}, batchSize) {List<EnumerationValue> batches ->
            enumerationValueCacheableRepository.saveAll(batches)
        }

        inBatches(dataFlows.findAll {!it.id}, batchSize) {List<DataFlow> batches ->
            dataFlowCacheableRepository.saveAll(batches)
        }
        inBatches(dataClassComponents.findAll {!it.id}, batchSize) {List<DataClassComponent> batches ->
            dataClassComponentCacheableRepository.saveAll(batches)
        }
        inBatches(dataElementComponents.findAll {!it.id}, batchSize) {List<DataElementComponent> batches ->
            dataElementComponentCacheableRepository.saveAll(batches)

        }

        annotations.keySet().sort().each {depth ->
            //annotations[depth].each {it.prePersist() }
            inBatches(annotations[depth].findAll {!it.id}, batchSize) {List<Annotation> batches ->
                annotationRepository.saveAll(batches)
            }
        }

        //edits.each {it.prePersist() }
        inBatches(edits.findAll {!it.id}, batchSize) {List<Edit> batches ->
            editCacheableRepository.saveAll(batches)
        }
        //referenceFiles.each {it.prePersist() }
        inBatches(referenceFiles.findAll {!it.id}, batchSize) {List<ReferenceFile> batches ->
            referenceFileRepository.saveAll(batches)
        }
        //rules.each {it.prePersist() }
        inBatches(rules.findAll {!it.id}, batchSize) {List<Rule> batches ->
            ruleRepository.saveAll(batches)
        }
        inBatches(ruleRepresentations.findAll {!it.id}, batchSize) {List<RuleRepresentation> batches ->
            ruleRepresentationCacheableRepository.saveAll(batches)
        }
        //semanticLinks.each {it.prePersist() }
        inBatches(semanticLinks.findAll {!it.id}, batchSize) {List<SemanticLink> batches ->
            semanticLinkRepository.saveAll(batches)
        }
        //summaryMetadata.each {it.prePersist() }
        inBatches(summaryMetadata.findAll {!it.id}, batchSize) {List<SummaryMetadata> batches ->
            summaryMetadataCacheableRepository.saveAll(batches)
        }
        inBatches(summaryMetadataReports.findAll {!it.id}, batchSize) {List<SummaryMetadataReport> batches ->
            summaryMetadataReportCacheableRepository.saveAll(batches)
        }
        //versionLinks.each {it.prePersist() }
        inBatches(versionLinks.findAll {!it.id}, batchSize) {List<VersionLink> batches ->
            versionLinkCacheableRepository.saveAll(batches)
        }

        Instant start = Instant.now()
        //metadata.each {it.prePersist() }
        inBatches(metadata.findAll {!it.id}, batchSize) {List<Metadata> batch ->
            metadataRepository.saveAll(batch)
        }
        printTimeTaken(start)

    }

    void setCreateProperties(CatalogueUser catalogueUser) {
        folders.keySet().sort().each {depth ->
            folders[depth].each {folder ->
                setCreateProperties(folder, catalogueUser)
            }
        }
        classificationSchemes.each {
            setCreateProperties(it, catalogueUser)
        }
        classifiers.each {
            setCreateProperties(it, catalogueUser)
        }
        terminologies.each {
            setCreateProperties(it, catalogueUser)
        }
        terms.each {
            setCreateProperties(it, catalogueUser)
        }
        termRelationshipTypes.each {
            setCreateProperties(it, catalogueUser)
        }
        termRelationships.each {
            setCreateProperties(it, catalogueUser)
        }
        codeSets.each {
            setCreateProperties(it, catalogueUser)
        }
        dataModels.each {
            setCreateProperties(it, catalogueUser)
        }
        dataClasses.keySet().sort().each {depth ->
            dataClasses[depth].each {
                setCreateProperties(it, catalogueUser)
            }
        }
        dataTypes.each {
            setCreateProperties(it, catalogueUser)
        }
        dataElements.each {
            setCreateProperties(it, catalogueUser)
        }
        enumerationValues.each {
            setCreateProperties(it, catalogueUser)
        }

        dataFlows.each {
            setCreateProperties(it, catalogueUser)
        }
        dataClassComponents.each {
            setCreateProperties(it, catalogueUser)
        }
        dataElementComponents.each {
            setCreateProperties(it, catalogueUser)
        }

        annotations.keySet().sort().each {depth ->
            annotations[depth].each {
                setCreateProperties(it, catalogueUser)
            }
        }
        edits.each {
            setCreateProperties(it, catalogueUser)
        }
        referenceFiles.each {
            setCreateProperties(it, catalogueUser)
        }
        rules.each {
            setCreateProperties(it, catalogueUser)
        }
        ruleRepresentations.each {
            setCreateProperties(it, catalogueUser)
        }
        semanticLinks.each {
            setCreateProperties(it, catalogueUser)
        }
        summaryMetadata.each {
            setCreateProperties(it, catalogueUser)
        }
        summaryMetadataReports.each {
            setCreateProperties(it, catalogueUser)
        }
        versionLinks.each {
            setCreateProperties(it, catalogueUser)
        }
        metadata.each {
            setCreateProperties(it, catalogueUser)
        }

    }

    static void setCreateProperties(Item item, CatalogueUser catalogueUser) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
        item.catalogueUser = catalogueUser
        if(item instanceof Facet && item.multiFacetAwareItem) {
            item.multiFacetAwareItemId = null
        }
        if(item instanceof Annotation && item.parentAnnotation) {
            item.parentAnnotationId = null
        }
    }


    boolean deleteWithContent() {

        Instant start = Instant.now()
        inBatches(metadata, batchSize) {List<Metadata> batch ->
            metadataRepository.deleteAll(batch)
        }
        printTimeTaken(start)
        inBatches(versionLinks, batchSize) {List<VersionLink> batch ->
            versionLinkCacheableRepository.deleteAll(batch)
        }
        inBatches(summaryMetadataReports, batchSize) {List<SummaryMetadataReport> batch ->
            summaryMetadataReportCacheableRepository.deleteAll(batch)
        }

        inBatches(summaryMetadata, batchSize) {List<SummaryMetadata> batch ->
            summaryMetadataCacheableRepository.deleteAll(batch)
        }
        inBatches(semanticLinks, batchSize) {List<SemanticLink> batch ->
            semanticLinkRepository.deleteAll(batch)
        }
        inBatches(ruleRepresentations, batchSize) {List<RuleRepresentation> batch ->
            ruleRepresentationCacheableRepository.deleteAll(ruleRepresentations)
        }
        inBatches(rules, batchSize) {List<Rule> batch ->
            ruleRepository.deleteAll(batch)
        }
        inBatches(referenceFiles, batchSize) {List<ReferenceFile> batch ->
            referenceFileRepository.deleteAll(batch)
        }
        inBatches(edits, batchSize) {List<Edit> batch ->
            editCacheableRepository.deleteAll(batch)
        }
        annotations.keySet().sort().reverse().each {depth ->
            inBatches(annotations[depth], batchSize) {List<Annotation> batch ->
                annotationRepository.deleteAll(batch)
            }
        }

        inBatches(dataElementComponents, batchSize) {List<DataElementComponent> batch ->
            // TODO: Do this in batch
            batch.each {dataElementComponent ->
                dataElementComponentCacheableRepository.removeSourceDataElements(dataElementComponent.id)
                dataElementComponentCacheableRepository.removeTargetDataElements(dataElementComponent.id)
            }
        }
        inBatches(dataElementComponents, batchSize) {List<DataElementComponent> batch ->
            dataElementComponentCacheableRepository.deleteAll(batch)
        }

        inBatches(dataClassComponents, batchSize) {List<DataClassComponent> batch ->
            batch.each {dataClassComponent ->
                dataClassComponentCacheableRepository.removeSourceDataClasses(dataClassComponent.id)
                dataClassComponentCacheableRepository.removeTargetDataClasses(dataClassComponent.id)
            }
        }
        inBatches(dataClassComponents, batchSize) {List<DataClassComponent> batch ->
            dataClassComponentCacheableRepository.deleteAll(batch)
        }
        inBatches(dataFlows, batchSize) {List<DataFlow> batch ->
            dataFlowCacheableRepository.deleteAll(batch)
        }
        inBatches(enumerationValues, batchSize) {List<EnumerationValue> batch ->
            enumerationValueCacheableRepository.deleteAll(batch)
        }
        inBatches(dataElements, batchSize) {List<DataElement> batch ->
            dataElementCacheableRepository.deleteAll(batch)
        }
        inBatches(dataTypes, batchSize) {List<DataType> batch ->
            dataTypeCacheableRepository.deleteAll(batch)
        }
        inBatches((List<DataClass>) dataClasses.values().flatten(), batchSize) {List<DataClass> batch ->
            dataClassCacheableRepository.deleteExtensionRelationships(batch.collect {DataClass it -> it.id})
        }
        dataClasses.keySet().sort().reverse().each {depth ->
            inBatches(dataClasses[depth], batchSize) {List<DataClass> batch ->
                dataClassCacheableRepository.deleteAll(batch)
            }
        }
        inBatches(dataModels, batchSize) {List<DataModel> batch ->
            dataModelCacheableRepository.deleteAll(batch)
        }
        inBatches(codeSets, batchSize) {List<CodeSet> batch ->
            codeSetCacheableRepository.removeAllAssociations(batch*.id)
        }
        inBatches(codeSets, batchSize) {List<CodeSet> batch ->
            codeSetCacheableRepository.deleteAll(batch)
        }
        inBatches(termRelationships, batchSize) {List<TermRelationship> batch ->
            termRelationshipCacheableRepository.deleteAll(batch)
        }
        inBatches(termRelationshipTypes, batchSize) {List<TermRelationshipType> batch ->
            termRelationshipTypeCacheableRepository.deleteAll(batch)
        }
        inBatches(terms, batchSize) {List<Term> batch ->
            termCacheableRepository.deleteAll(batch)
        }
        inBatches(terminologies, batchSize) {List<Terminology> batch ->
            terminologyCacheableRepository.deleteAll(batch)
        }
        inBatches(classifiers, batchSize) {List<Classifier> batch ->
            classifierCacheableRepository.deleteAllJoinAdministeredItemToClassifierIds(batch*.id)
        }
        inBatches(classifiers, batchSize) {List<Classifier> batch ->
            classifierCacheableRepository.deleteAll(batch)
        }
        inBatches(classificationSchemes, batchSize) {List<ClassificationScheme> batch ->
            classificationSchemeCacheableRepository.deleteAll(batch)
        }
        folders.keySet().sort().reverse().each {depth ->
            inBatches(folders[depth], batchSize) {List<Folder> batch ->
                folderCacheableRepository.deleteAll(batch)
            }
        }

        true

    }

    void shredFacets(AdministeredItem item) {
        if(item.metadata) {
            item.metadata.each {
                it.multiFacetAwareItem = item
                it.multiFacetAwareItemDomainType = item.domainType
            }
            metadata.addAll(item.metadata)
        }
        if(item instanceof Model && item.versionLinks) {
            versionLinks.addAll(item.versionLinks)
        }
        if(item.summaryMetadata) {
            summaryMetadata.addAll(item.summaryMetadata)
            item.summaryMetadata.each {sm ->
                if(sm.summaryMetadataReports) {
                    summaryMetadataReports.addAll(sm.summaryMetadataReports)
                }
            }
        }
        if(item.semanticLinks) {
            semanticLinks.addAll(item.semanticLinks)
        }
        if(item.rules) {
            rules.addAll(item.rules)
            item.rules.each {r ->
                if(r.ruleRepresentations) {
                    ruleRepresentations.addAll(r.ruleRepresentations)
                }
            }
        }
        if(item.referenceFiles) {
            referenceFiles.addAll(item.referenceFiles)
        }
        if(item.edits) {
            edits.addAll(item.edits)
        }
        if(item.annotations) {
            item.annotations.each {
                shred(it, 0)
            }
        }
    }

    void shred(Annotation annotation, Integer depth = 0) {
        if (annotations[depth]) {
            annotations[depth].add(annotation)
        } else {
            annotations[depth] = [annotation] as Set
        }
        annotation.childAnnotations.each {
            shred(it, depth + 1)
        }
    }


    static void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        log.info(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }

    private static <T> void inBatches(final List<T> items, final int batchSize, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) return
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

    private static <T> void inBatches(final Set<T> items, final int batchSize, @DelegatesTo(List) Closure saver) {
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

    Folder loadWithContent(Folder folder) {
        folders[0] = [folder] as Set
        loadContent()
        return folders[0].first()
    }

    ClassificationScheme loadWithContent(ClassificationScheme classificationScheme) {
        classificationSchemes = [classificationScheme] as Set
        loadContent()
        return classificationScheme
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

    void loadContent() {
        if (folders[0]) {
            int depth = 1
            Set<UUID> foundFolders = folders[0]*.id as Set<UUID>
            do {
                List<Folder> retrievedFolders = inBatchesRead(foundFolders as List<UUID>, batchSize) {List batch ->
                    folderCacheableRepository.readAllByFolderIdIn(batch)
                }
                foundFolders = retrievedFolders*.id as Set
                if (foundFolders) {
                    folders[depth] = retrievedFolders as Set
                }
                depth++
            } while (foundFolders.size() > 0)
        }
        final Set<Folder> foldersValuesFlatten = (Set<Folder>) folders.values().flatten()
        allItems.putAll(foldersValuesFlatten.collectEntries {[it.id, it]})
        if (foldersValuesFlatten) {
            classificationSchemes = inBatchesReadSet(foldersValuesFlatten*.id, batchSize) {List<UUID> batch ->
                classificationSchemeCacheableRepository.readAllByFolderIdIn(batch)
            }
        }
        allItems.putAll(classificationSchemes.collectEntries {[it.id, it]})
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


}
