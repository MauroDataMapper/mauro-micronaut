package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.persistence.facet.RuleRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassifierRepository
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.facet.SummaryMetadataRepository

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
    ClassifierRepository classifierRepository


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
        associations.reverse().each {association ->
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
        List<Metadata> metadata = []

        items.each {item ->
            if (item.metadata) {
                metadata.addAll(item.metadata)
                metadataRepository.deleteAll(metadata)
            }
        }
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
                    item.classifiers.each{ classifierRepository.deleteJoinAdministeredItemToClassifier(item, it.id)}
                }
            }
        }
    }


    @NonNull
    AdministeredItemCacheableRepository getRepository(AdministeredItem item) {
        cacheableRepositories.find {it.handles(item.class)}
    }

    void deleteSummaryMetadata(Collection<AdministeredItem> items) {
        List<SummaryMetadata> summaryMetadata = []
        List<SummaryMetadataReport> summaryMetadataReports = []
        items.each { item ->
            if (item.summaryMetadata){
                item.summaryMetadata.each {
                    if (it.summaryMetadataReports){
                        summaryMetadataReports.addAll(it.summaryMetadataReports)
                    }
                    summaryMetadata.add(it)
                }
                summaryMetadataRepository.deleteAll(summaryMetadata)
                summaryMetadataReportCacheableRepository.deleteAll(summaryMetadataReports)
            }
        }
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
                annotationCacheableRepository.deleteAll(annotations)
            }
        }
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
}

