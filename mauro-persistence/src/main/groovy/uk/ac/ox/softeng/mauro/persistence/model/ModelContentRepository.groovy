package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

@Slf4j
@CompileStatic
@Singleton
class ModelContentRepository<M extends Model> extends AdministeredItemContentRepository {

    M findWithContentById(UUID id) {
        (M) administeredItemRepository.findById(id)
    }

    M saveWithContent(@NonNull M model) {
        List<List<AdministeredItem>> associations = model.getAllAssociations()

        M saved = (M) getRepository(model).save(model)
        saveAllFacets(saved)
        associations.each {association ->
            if (association) {
                List<AdministeredItem> savedAssociation = getRepository(association.first()).saveAll((List<AdministeredItem>) association)
                saveAllFacets(savedAssociation)
            }
        }
        saved
    }

    List<Metadata> saveAllFacets(@NonNull AdministeredItem item) {
        saveAllFacets([item])
    }

    List<Metadata> saveAllFacets(List<AdministeredItem> items) {
        List<Metadata> metadata = []

        items.each {item ->
            if (item.metadata) {
                item.metadata.each {
                    it.multiFacetAwareItemDomainType = item.domainType
                    it.multiFacetAwareItemId = item.id
                    it.multiFacetAwareItem = item
                }
                metadata.addAll(item.metadata)
            }
        }

        metadataRepository.saveAll(metadata)
    }
}