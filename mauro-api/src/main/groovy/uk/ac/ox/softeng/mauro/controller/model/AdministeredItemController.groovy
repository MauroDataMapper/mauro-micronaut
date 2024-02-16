package uk.ac.ox.softeng.mauro.controller.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

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

    CacheableAdministeredItemRepository<I> administeredItemRepository

    CacheableAdministeredItemRepository<P> parentItemRepository

    AdministeredItemContentRepository<I> administeredItemContentRepository

    @Inject
    PathRepository pathRepository

    AdministeredItemController(Class<I> itemClass, CacheableAdministeredItemRepository<I> administeredItemRepository, CacheableAdministeredItemRepository<P> parentItemRepository, AdministeredItemContentRepository<I> administeredItemContentRepository) {
        this.itemClass = itemClass
        this.administeredItemRepository = administeredItemRepository
        this.parentItemRepository = parentItemRepository
        this.administeredItemContentRepository = administeredItemContentRepository
    }

    I show(UUID id) {
        I item = administeredItemRepository.findById(id)
        pathRepository.readParentItems(item)
        item.updatePath()
        item
    }

    @Transactional
    I create(UUID parentId, @Body @NonNull I item) {
        cleanBody(item)

        P parent = parentItemRepository.readById(parentId)
        createEntity(parent, item)
    }

    protected AdministeredItem updateCreationProperties(AdministeredItem item) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
        item.createdBy = 'USER@example.org'
        item
    }

    protected I createEntity(@NonNull P parent, @Body @NonNull I cleanItem) {
        cleanItem.parent = parent
        updateCreationProperties(cleanItem)
        pathRepository.readParentItems(cleanItem)
        cleanItem.updatePath()
        administeredItemRepository.save(cleanItem)
    }

    I update(UUID id, @Body @NonNull I item) {
        cleanBody(item)
        I existing = administeredItemRepository.readById(id)
        updateEntity(existing, item)
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

    protected I updateEntity(I existing, @NonNull I cleanItem) {
        boolean hasChanged = updateProperties(existing, cleanItem)
        pathRepository.readParentItems(existing)
        existing.updatePath()

        if (hasChanged) {
            administeredItemRepository.update(existing)
        } else {
            existing
        }
    }

    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable I item) {
        I itemToDelete = itemClass.getDeclaredConstructor().newInstance()
        itemToDelete.id = id
        itemToDelete.version = item?.version
        Long deleted = administeredItemContentRepository.deleteWithContent(itemToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    ListResponse<I> list(UUID parentId) {
        P parent = parentItemRepository.readById(parentId)
        if (!parent) return null
        List<I> items = administeredItemRepository.readAllByParent(parent)
        items.each {
            pathRepository.readParentItems(it)
        }
        items.each {
            it.updatePath()
        }
        ListResponse.from(items)
    }

    protected I cleanBody(I item) {
        I defaultItem = (I) item.class.getDeclaredConstructor().newInstance()

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
