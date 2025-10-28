package org.maurodata.persistence

import groovy.util.logging.Slf4j
import groovyjarjarantlr4.v4.runtime.misc.OrderedHashSet
import jakarta.inject.Inject
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.CatalogueFile
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
import org.maurodata.persistence.facet.AnnotationRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.facet.ReferenceFileRepository
import org.maurodata.persistence.facet.RuleRepository
import org.maurodata.persistence.facet.SemanticLinkRepository
import org.maurodata.persistence.folder.FolderRepository
import org.maurodata.persistence.terminology.TermRelationshipRepository

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

    Set<Metadata> metadata = []
    Map<Integer, Set<Annotation>> annotations = [:]
    //List<CatalogueFile> catalogueFiles = []
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

    void saveWithContent() {
        folders.keySet().sort().each {depth ->
            folderCacheableRepository.saveAll(folders[depth].findAll {!it.id})
        }
        classificationSchemeCacheableRepository.saveAll(classificationSchemes.findAll {!it.id})
        classifierCacheableRepository.saveAll(classifiers.findAll {!it.id})
        terminologyCacheableRepository.saveAll(terminologies.findAll {!it.id})
        termCacheableRepository.saveAll(terms.findAll {!it.id})
        termRelationshipTypeCacheableRepository.saveAll(termRelationshipTypes.findAll {!it.id})
        termRelationshipCacheableRepository.saveAll(termRelationships.findAll {!it.id})
        codeSetCacheableRepository.saveAll(codeSets.findAll {!it.id})
        dataModelCacheableRepository.saveAll (dataModels.findAll {!it.id})
        dataClasses.keySet().sort().each {depth ->
            dataClassCacheableRepository.saveAll (dataClasses[depth].findAll {!it.id})
        }
        dataClasses.values().flatten().each {DataClass dataClass ->
            dataClass.extendsDataClasses.each {superClass ->
                dataClassCacheableRepository.addDataClassExtensionRelationship(dataClass.id, superClass.id)
            }
        }
        dataTypeCacheableRepository.saveAll (dataTypes.findAll {!it.id})
        dataElementCacheableRepository.saveAll (dataElements.findAll {!it.id})
        enumerationValueCacheableRepository.saveAll (enumerationValues.findAll {!it.id})

        annotations.keySet().sort().each {depth ->
            //annotations[depth].each {it.prePersist() }
            annotationRepository.saveAll(annotations[depth].findAll {!it.id})
        }

        //edits.each {it.prePersist() }
        editCacheableRepository.saveAll(edits.findAll {!it.id})
        //referenceFiles.each {it.prePersist() }
        referenceFileRepository.saveAll(referenceFiles.findAll {!it.id})
        //rules.each {it.prePersist() }
        ruleRepository.saveAll(rules.findAll {!it.id})
        ruleRepresentationCacheableRepository.saveAll(ruleRepresentations.findAll {!it.id})
        //semanticLinks.each {it.prePersist() }
        semanticLinkRepository.saveAll(semanticLinks.findAll {!it.id})
        //summaryMetadata.each {it.prePersist() }
        summaryMetadataCacheableRepository.saveAll(summaryMetadata.findAll {!it.id})
        summaryMetadataReportCacheableRepository.saveAll(summaryMetadataReports.findAll {!it.id})
        //versionLinks.each {it.prePersist() }
        versionLinkCacheableRepository.saveAll(versionLinks.findAll {!it.id})

        Instant start = Instant.now()
        //metadata.each {it.prePersist() }
        inBatches(metadata.findAll {!it.id} as List, 1000) { List<Metadata> batch ->
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

    void setCreateProperties(Item item, CatalogueUser catalogueUser) {
        //item.id = null
        //item.version = null
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
        inBatches(metadata as List, 1000) { List<Metadata> batch ->
            metadataRepository.deleteAll(batch)
        }
        printTimeTaken(start)
        versionLinkCacheableRepository.deleteAll(versionLinks)
        summaryMetadataReportCacheableRepository.deleteAll(summaryMetadataReports)
        summaryMetadataCacheableRepository.deleteAll(summaryMetadata)
        semanticLinkRepository.deleteAll(semanticLinks)
        ruleRepresentationCacheableRepository.deleteAll(ruleRepresentations)
        ruleRepository.deleteAll(rules)
        referenceFileRepository.deleteAll(referenceFiles)
        editCacheableRepository.deleteAll(edits)
        annotations.keySet().sort().reverse().each {depth ->
            annotationRepository.deleteAll(annotations[depth])
        }
        enumerationValueCacheableRepository.deleteAll (enumerationValues)
        dataElementCacheableRepository.deleteAll (dataElements)
        dataTypeCacheableRepository.deleteAll (dataTypes)

        dataClassCacheableRepository.deleteExtensionRelationships(dataClasses.values().flatten().collect {DataClass it -> it.id})
        dataClasses.keySet().sort().reverse().each {depth ->
            dataClassCacheableRepository.deleteAll (dataClasses[depth])
        }
        dataModelCacheableRepository.deleteAll (dataModels)
        codeSetCacheableRepository.removeAllAssociations(codeSets.id)
        codeSetCacheableRepository.deleteAll(codeSets)
        termRelationshipCacheableRepository.deleteAll(termRelationships)
        termRelationshipTypeCacheableRepository.deleteAll(termRelationshipTypes)
        termCacheableRepository.deleteAll(terms)
        terminologyCacheableRepository.deleteAll(terminologies)
        classifierCacheableRepository.deleteAllJoinAdministeredItemToClassifierIds(classifiers.id)
        classifierCacheableRepository.deleteAll(classifiers)
        classificationSchemeCacheableRepository.deleteAll(classificationSchemes)
        folders.keySet().sort().reverse().each {depth ->
            folderCacheableRepository.deleteAll(folders[depth])
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


    void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        System.err.println(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }

    private static <T> void inBatches(List<T> items, int batchSize, @DelegatesTo(List) Closure saver) {
        if (items == null || items.isEmpty()) return
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size())
            // Defensive copy so the batch does not retain a view of the full list
            List<T> batch = new ArrayList<>(items.subList(i, end))
            saver.call(batch)
        }
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


    void loadContent() {
        if(folders[0]) {
            int depth = 1
            Set<UUID> foundFolders = folders[0].id
            do {
                List<Folder> retrievedFolders = folderCacheableRepository.readAllByFolderIdIn(foundFolders)
                foundFolders = retrievedFolders.id as Set
                if(foundFolders) {
                    folders[depth] = retrievedFolders as Set
                }
                depth++
            } while (foundFolders.size() > 0)
        }
        allItems.putAll(folders.values().flatten().collectEntries { [it.id, it]})
        if(folders.values().flatten()) {
            classificationSchemes = classificationSchemeCacheableRepository.readAllByFolderIdIn(folders.values().flatten().id as Set<UUID>)
        }
        allItems.putAll(classificationSchemes.collectEntries{[it.id, it]})
        if(classificationSchemes) {
            classifiers = classifierCacheableRepository.readAllByClassificationSchemeIdIn(classificationSchemes.id)
        }
        allItems.putAll(classifiers.collectEntries{[it.id, it]})

        if(folders.values().flatten()) {
            terminologies = terminologyCacheableRepository.readAllByFolderIdIn(folders.values().flatten().id as Set<UUID>)
        }
        allItems.putAll(terminologies.collectEntries{[it.id, it]})

        if(terminologies) {
            terms = termCacheableRepository.readAllByTerminologyIdIn(terminologies.id)
            allItems.putAll(terms.collectEntries{[it.id, it]})

            termRelationshipTypes = termRelationshipTypeCacheableRepository.readAllByTerminologyIdIn(terminologies.id)
            allItems.putAll(termRelationshipTypes.collectEntries{[it.id, it]})

            termRelationships = termRelationshipCacheableRepository.readAllByTerminologyIdIn(terminologies.id)
            allItems.putAll(termRelationships.collectEntries{[it.id, it]})
        }

        if(folders.values().flatten()) {
            codeSets = codeSetCacheableRepository.readAllByFolderIdIn(folders.values().flatten().id as Set<UUID>)
        }
        allItems.putAll(codeSets.collectEntries {[it.id, it]})

        if(folders.values().flatten()) {
            dataModels = dataModelCacheableRepository.readAllByFolderIdIn(folders.values().flatten().id as Set<UUID>)
        }
        allItems.putAll(dataModels.collectEntries {[it.id, it]})
        if(dataModels) {
            dataClasses[0] = dataClassCacheableRepository.readAllByDataModelIdInAndParentDataClassIsNull(dataModels.id) as Set

            int depth = 1
            Set<UUID> foundClasses = dataClasses[0].id as Set
            while (foundClasses.size() > 0) {
                List<DataClass> retrievedDataClasses = dataClassCacheableRepository.readAllByParentDataClassIdIn(foundClasses)
                foundClasses = retrievedDataClasses.id as Set
                if (foundClasses) {
                    dataClasses[depth] = retrievedDataClasses as Set
                }
                depth++
            }
            allItems.putAll(dataClasses.values().flatten().collectEntries {[it.id, it]})

            Map<UUID, DataClass> dataClassMap = dataClasses.values().flatten().collectEntries{[it.id, it]}

            List<DataClassExtensionDTO> extensions = dataClassCacheableRepository.getDataClassExtensionRelationships(dataClasses.values().flatten().id)
            extensions.each {
                dataClassMap[it.dataClassId].extendsDataClasses.add(dataClassMap[it.extendedDataClassId])
            }

            dataTypes = dataTypeCacheableRepository.readAllByDataModelIdIn(dataModels.id)
            allItems.putAll(dataTypes.collectEntries {[it.id, it]})
        }

        if(dataTypes) {
            enumerationValues = enumerationValueCacheableRepository.readAllByEnumerationTypeIdIn(dataTypes.id)
            allItems.putAll(enumerationValues.collectEntries {[it.id, it]})
        }

        if(dataClasses.values().flatten()) {
            dataElements = dataElementCacheableRepository.readAllByDataClassIdIn(dataClasses.values().flatten().id)
            allItems.putAll(dataElements.collectEntries{[it.id, it]})
        }

        // annotations
        annotations[0] = annotationCacheableRepository.readAllByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull(allItems.values().id)
        int depth = 1
        Set<UUID> foundAnnotations = annotations[0].id
        do {
            Set<Annotation> retrievedAnnotations = annotationCacheableRepository.readAllByParentAnnotationIdIn(foundAnnotations)
            foundAnnotations = retrievedAnnotations.id
            if (foundAnnotations) {
                annotations[depth] = retrievedAnnotations
            }
            depth++
        } while (foundAnnotations.size() > 0)


        edits = editCacheableRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)
        referenceFiles = referenceFileRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)
        rules = ruleRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)
        ruleRepresentations = ruleRepresentationCacheableRepository.readAllByRuleIdIn(rules.id)
        semanticLinks = semanticLinkRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)
        summaryMetadata = summaryMetadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)
        summaryMetadataReports = summaryMetadataReportCacheableRepository.readAllBySummaryMetadataIdIn(summaryMetadata.id)
        versionLinks = versionLinkCacheableRepository.readAllByMultiFacetAwareItemIdIn(summaryMetadata.id)

        metadata = metadataCacheableRepository.readAllByMultiFacetAwareItemIdIn(allItems.values().id)

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
            ((Terminology) allItems[term.terminology.id]).terms.add(term)
        }
        termRelationshipTypes.each {termRelationshipType ->
            ((Terminology) allItems[termRelationshipType.terminology.id]).termRelationshipTypes.add(termRelationshipType)
        }
        termRelationships.each {termRelationship ->
            ((Terminology) allItems[termRelationship.terminology.id]).termRelationships.add(termRelationship)
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
                if(dataClass.parentDataClass) {
                    ((DataClass) allItems[dataClass.parentDataClass.id]).dataClasses.add(dataClass)
                } else {
                    ((DataModel) allItems[dataClass.dataModel.id]).dataClasses.add(dataClass)
                }
            }
        }
        dataTypes.each {dataType ->
            ((DataModel) allItems[dataType.dataModel.id]).dataTypes.add(dataType)
        }
        enumerationValues.each {enumerationValue ->
            ((DataType) allItems[enumerationValue.enumerationType.id]).enumerationValues.add(enumerationValue)
        }

        dataElements.each {dataElement ->
            ((DataClass) allItems[dataElement.dataClass.id]).dataElements.add(dataElement)
        }

        edits.each {edit ->
            allItems[edit.multiFacetAwareItemId].edits.add(edit)
        }

        annotations[0].each {annotation ->
            allItems[annotation.multiFacetAwareItemId].annotations.add(annotation)
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
