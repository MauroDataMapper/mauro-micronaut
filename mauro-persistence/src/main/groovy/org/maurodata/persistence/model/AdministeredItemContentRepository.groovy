package org.maurodata.persistence.model

import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.classifier.ClassifierRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.facet.RuleRepository
import org.maurodata.persistence.facet.SummaryMetadataRepository
import org.maurodata.persistence.facet.VersionLinkRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class AdministeredItemContentRepository {

    @Inject
    List<AdministeredItemCacheableRepository> cacheableRepositories

    @Inject
    MetadataRepository metadataRepository

    @Inject
    SummaryMetadataRepository summaryMetadataRepository

    @Inject
    RuleRepository ruleRepository

    @Inject
    ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository

    @Inject
    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    @Inject
    FacetCacheableRepository.AnnotationCacheableRepository annotationCacheableRepository

    @Inject
    FacetCacheableRepository.ReferenceFileCacheableRepository referenceFileCacheableRepository

    @Inject
    FacetCacheableRepository.VersionLinkCacheableRepository versionLinkCacheableRepository

    @Inject
    FacetCacheableRepository.SemanticLinkCacheableRepository semanticLinkCacheableRepository

    @Inject
    ClassifierRepository classifierRepository

    @Inject
    VersionLinkRepository versionLinkRepository

    AdministeredItemRepository administeredItemRepository

    /**
     * Read AdministeredItem with all Contents.
     *
     * Contents includes Child and in some cases Sibling relationships.
     */
    AdministeredItem readWithContentById(UUID id) {
        administeredItemRepository.readById(id)
    }

    /**
     * Delete AdministeredItem and all child Contents and Facets and classifiers
     */
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        List<Collection<AdministeredItem>> associations = administeredItem.getAllAssociations()

        // delete the association contents in reverse order
        associations.reverse().each { association ->
            if (association) {
                deleteAllJoinAdministeredItemToClassifier(association)
                deleteAllFacets(association)
                getRepository(association.first()).deleteAll(association)
            }
        }
        deleteAllFacets(administeredItem)
        deleteAllJoinAdministeredItemToClassifier(administeredItem)
        Long result = getRepository(administeredItem).delete(administeredItem)
        result
    }

    void deleteAllFacets(@NonNull AdministeredItem item) {
        deleteAllFacets([item])
    }

    void deleteAllFacets(Collection<AdministeredItem> items) {
        deleteMetadata(items)
        deleteSummaryMetadata(items)
        deleteAnnotations(items)
        deleteReferenceFiles(items)
    }

    void deleteAllJoinAdministeredItemToClassifier(@NonNull AdministeredItem item) {
        deleteAllJoinAdministeredItemToClassifier([item])
    }

    void deleteAllJoinAdministeredItemToClassifier(Collection<AdministeredItem> items) {
        items.each { item ->
            //when adminItem is classifier, delete all rows with that classifierId in the JoinAdministeredItemToClassifier table
            if (item.domainType == Classifier.class.simpleName) {
                classifierRepository.deleteAllJoinAdministeredItemToClassifier(item as Classifier)
            } else {
                if (item.classifiers) {
                    item.classifiers.each { classifierRepository.deleteJoinAdministeredItemToClassifier(item, it.id) }
                }
            }
        }
    }


    @NonNull
    AdministeredItemCacheableRepository getRepository(AdministeredItem item) {
        cacheableRepositories.find { it.handles(item.class) || it.handles(item.domainType) }
    }

    void deleteMetadata(Collection<AdministeredItem> items) {
        List<Metadata> metadata = []
        items.each { item ->
            if (item.metadata) {
                metadata.addAll(item.metadata)
            }
        }
        metadataRepository.deleteAll(metadata)
    }

    void deleteSummaryMetadata(Collection<AdministeredItem> items) {
        List<SummaryMetadata> summaryMetadata = []
        List<SummaryMetadataReport> summaryMetadataReports = []
        items.each { item ->
            if (item.summaryMetadata) {
                item.summaryMetadata.each {
                    if (it.summaryMetadataReports) {
                        summaryMetadataReports.addAll(it.summaryMetadataReports)
                    }
                    summaryMetadata.add(it)
                }
            }
        }
        summaryMetadataRepository.deleteAll(summaryMetadata)
        summaryMetadataReportCacheableRepository.deleteAll(summaryMetadataReports)
    }

    void deleteAnnotations(Collection<AdministeredItem> items) {
        List<Annotation> annotations = []
        items.each { item ->
            if (item.annotations) {
                item.annotations.each {
                    if (it.childAnnotations) {
                        annotations.addAll(it.childAnnotations)
                    }
                    annotations.add(it)
                }
            }
        }
        annotationCacheableRepository.deleteAll(annotations)
    }

    void deleteReferenceFiles(Collection<AdministeredItem> items) {
        List<ReferenceFile> referenceFiles = []
        items.each { item ->
            if (item.referenceFiles) {
                referenceFiles.addAll(item.referenceFiles)
            }
        }
        referenceFiles.each {
            referenceFileCacheableRepository.deleteById(it.id)
        }
    }

    AdministeredItem saveWithContent(@NonNull AdministeredItem model) {
        List<Collection<AdministeredItem>> associations = model.getAllAssociations()
        AdministeredItem saved = (AdministeredItem) getRepository(model).save(model)

        saveAllFacets(saved)
        associations.each { association ->
            if (association) {
                Collection<AdministeredItem> savedAssociation = getRepository(association.first()).saveAll((Collection<AdministeredItem>) association)
                saveAllFacets(savedAssociation)
            }
        }
        saved
    }

    void saveAllFacets(@NonNull AdministeredItem item) {
        saveAllFacets([item])
    }

    void saveAllFacets(List<AdministeredItem> items) {
        List<Metadata> metadata = []

        items.each { item ->
            if (item.metadata) {
                item.metadata.each {
                    updateMultiAwareData(item, it)
                }
                metadata.addAll(item.metadata)
            }
        }
        metadataRepository.saveAll(metadata)
        saveSummaryMetadataFacets(items)
        saveAnnotations(items)
        saveReferenceFiles(items)
        saveRules(items)
        saveSemanticLinks(items)
    }

    void saveSummaryMetadataFacets(List<AdministeredItem> items) {
        List<SummaryMetadata> summaryMetadata = []
        List<SummaryMetadataReport> summaryMetadataReports = []
        SummaryMetadata saved
        items.each { item ->
            if (item.summaryMetadata) {
                item.summaryMetadata.each {
                    updateMultiAwareData(item, it)
                    saved = summaryMetadataRepository.save(it)
                    if (it.summaryMetadataReports) {
                        it.summaryMetadataReports.forEach {
                            report ->
                                report.summaryMetadataId = saved.id
                        }
                        summaryMetadataReports.addAll(summaryMetadataReportCacheableRepository.saveAll(it.summaryMetadataReports))
                    }
                    summaryMetadata.add(saved)
                }
            }
        }
    }

    void saveAnnotations(List<AdministeredItem> items) {
        List<Annotation> annotations = []
        items.each { item ->
            if (item.annotations) {
                item.annotations.each {
                    updateMultiAwareData(item, it)
                    if (it.childAnnotations) {
                        it.childAnnotations.forEach { child ->
                            updateMultiAwareData(item, child)
                            child.parentAnnotationId = it.id
                        }
                    }
                }
                annotations.addAll(item.annotations)
            }
        }
        annotationCacheableRepository.saveAll(annotations)
    }

    void saveReferenceFiles(List<AdministeredItem> items) {
        List<ReferenceFile> referenceFiles = []
        items.each { item ->
            if (item.referenceFiles) {
                item.referenceFiles.each {
                    updateMultiAwareData(item, it)
                }
                referenceFiles.addAll(item.referenceFiles)
            }
        }
        referenceFileCacheableRepository.saveAll(referenceFiles)
    }

    void saveRules(List<AdministeredItem> items) {
        List<Rule> rules = []
        List<RuleRepresentation> ruleRepresentations = []
        Rule saved
        items.each { item ->
            if (item.rules) {
                item.rules.each {
                    updateMultiAwareData(item, it)
                    saved = ruleRepository.save(it)
                    if (it.ruleRepresentations) {
                        it.ruleRepresentations.forEach {
                            representation ->
                                representation.ruleId = saved.id
                        }
                        ruleRepresentations.addAll(ruleRepresentationCacheableRepository.saveAll(it.ruleRepresentations))
                    }
                    rules.add(saved)
                }
            }
        }
    }

    void saveSemanticLinks(List<AdministeredItem> items) {
        List<SemanticLink> semanticLinks = []
        items.each { item ->
            if (item.semanticLinks) {
                item.semanticLinks.each {
                    updateMultiAwareData(item, it)
                }
                semanticLinks.addAll(item.semanticLinks)
            }
        }
        semanticLinkCacheableRepository.saveAll(semanticLinks)
    }

    private void updateMultiAwareData(AdministeredItem item, Facet it) {
        it.multiFacetAwareItemDomainType = item.domainType
        it.multiFacetAwareItemId = item.id
        it.multiFacetAwareItem = item
    }


}

