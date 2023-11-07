package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Mono

import java.util.function.BiFunction

@CompileStatic
abstract class AdministeredItemController<I extends AdministeredItem, P extends AdministeredItem> {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        ['class'] +
        ['id', 'version', 'dateCreated', 'lastUpdated', 'domainType', 'createdBy', 'path', /*'breadcrumbTree',*/ 'parent', 'owner']
    }

    /**
     * Properties disallowed in a simple create request.
     */
    List<String> getDisallowedCreateProperties() {
        disallowedProperties
    }

    Class<I> itemClass

    AdministeredItemRepository<I> administeredItemRepository

    AdministeredItemRepository<P> parentItemRepository

    AdministeredItemContentRepository<I> administeredItemContentRepository

    @Inject
    PathRepository pathRepository

    AdministeredItemController(Class<I> itemClass, AdministeredItemRepository<I> administeredItemRepository, AdministeredItemRepository<P> parentItemRepository, AdministeredItemContentRepository<I> administeredItemContentRepository) {
        this.itemClass = itemClass
        this.administeredItemRepository = administeredItemRepository
        this.parentItemRepository = parentItemRepository
        this.administeredItemContentRepository = administeredItemContentRepository
    }

    Mono<I> show(UUID parentId, UUID id) {
        administeredItemRepository.findByParentIdAndId(parentId, id).flatMap {I item ->
            pathRepository.readParentItems(item).map {
                item.updatePath()
                item
            }
        }
    }

    @Transactional
    Mono<I> create(UUID parentId, @Body @NonNull I item) {
        cleanBody(item)

        parentItemRepository.readById(parentId).flatMap {P parent ->
            createEntity(parent, item)
        }
    }

    protected AdministeredItem updateCreationProperties(AdministeredItem item) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
        item.createdBy = 'USER@example.org'
        item
    }

    protected Mono<I> createEntity(P parent, @Body @NonNull I cleanItem) {
        cleanItem.parent = parent
        updateCreationProperties(cleanItem)
        pathRepository.readParentItems(cleanItem).flatMap {
            cleanItem.updatePath()
            administeredItemRepository.save(cleanItem)
        }
    }

    Mono<I> update(UUID parentId, UUID id, @Body @NonNull I item) {
        cleanBody(item)

        administeredItemRepository.readById(id).flatMap {I existing ->
            updateEntity(existing, item)
        }
    }

    protected boolean updateProperties(I existing, I cleanItem) {
        boolean hasChanged
        existing.properties.each {
            String key = it.key
            if (!disallowedProperties.contains(key) && cleanItem[key] != null && existing.hasProperty(key).properties.setter) {
                if (existing[key] != cleanItem[key]) {
                    existing[key] = cleanItem[key]
                    hasChanged = true
                }
            }
        }
        return hasChanged
    }

    protected Mono<I> updateEntity(I existing, @Body @NonNull I cleanItem) {
        boolean hasChanged = updateProperties(existing, cleanItem)

        if (hasChanged) {
            pathRepository.readParentItems(existing).flatMap {
                existing.updatePath()
                administeredItemRepository.update(existing)
            }
        } else {
            pathRepository.readParentItems(existing).map {
                existing.updatePath()
                existing
            }
        }
    }

    @Transactional
    Mono<HttpStatus> delete(UUID parentId, UUID id, @Body @Nullable I item) {
        I itemToDelete = itemClass.getDeclaredConstructor().newInstance()
        itemToDelete.id = id
        itemToDelete.version = item?.version
        administeredItemContentRepository.deleteWithContent(itemToDelete).map {Long deleted ->
            if (deleted) {
                HttpStatus.NO_CONTENT
            } else {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
            }
        }
    }

    Mono<ListResponse<I>> list(UUID parentId) {
        parentItemRepository.readById(parentId).flatMap {P parent ->
            administeredItemRepository.readAllByParent(parent).flatMap {I item ->
                Mono.zip(Mono.just(item), pathRepository.readParentItems(item), (BiFunction<I, List<AdministeredItem>, I>) {it, _ -> it})
            }.collectList().map {List<I> items ->
                items.each {((AdministeredItem) it).updatePath()}
                ListResponse.from(items)
            }
        }
    }

    protected I cleanBody(I item) {
        I defaultItem = itemClass.getDeclaredConstructor().newInstance()

        // Disallowed properties cannot be set by user request
        disallowedCreateProperties.each {String key ->
            if (defaultItem.hasProperty(key).properties.setter && item[key] != defaultItem[key]) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Property $key cannot be set directly")
            }
        }

        // Collection properties cannot be set in user requests as these might be used in services
        defaultItem.properties.each {
            String key = it.key
            if (defaultItem.hasProperty(key).properties.setter && (it.value instanceof Collection || it.value instanceof Map)) {
                if (item[key]) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Collection or Map $key cannot be set directly")
                item[key] = null
            }
        }

        item
    }
}
