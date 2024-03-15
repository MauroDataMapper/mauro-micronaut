package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
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

    AdministeredItemCacheableRepository administeredItemRepository

    /**
     * Read AdministeredItem with all Contents.
     *
     * Contents includes Child and in some cases Sibling relationships.
     */
    AdministeredItem readWithContentById(UUID id) {
        administeredItemRepository.readById(id)
    }

    /**
     * Delete AdministeredItem and all child Contents and Facets.
     */
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        List<Collection<AdministeredItem>> associations = administeredItem.getAllAssociations()

        // delete the association contents in reverse order
        associations.reverse().each {association ->
            if (association) {
                getRepository(association.first()).deleteAll(association)
                deleteAllFacets(association)
            }
        }
        deleteAllFacets(administeredItem)
        getRepository(administeredItem).delete(administeredItem)
    }

    Long deleteAllFacets(@NonNull AdministeredItem item) {
        deleteAllFacets([item])
    }

    Long deleteAllFacets(Collection<AdministeredItem> items) {
        List<Metadata> metadata = []

        items.each {item ->
            if (item.metadata) {
                metadata.addAll(item.metadata)
            }
        }

        metadataRepository.deleteAll(metadata)
        deleteSummaryMetadata(items)
    }

    @NonNull
    AdministeredItemCacheableRepository getRepository(AdministeredItem item) {
        cacheableRepositories.find {it.handles(item.class)}
    }

    void deleteSummaryMetadata(List<AdministeredItem> items) {
        List<SummaryMetadata> summaryMetadata = []
        items.each { item ->
            if (item.summaryMetadata){
                summaryMetadata.addAll(item.summaryMetadata)
            }
        }
        summaryMetadataRepository.deleteAll(summaryMetadata)
    }
}
