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
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
abstract class AdministeredItemController<I extends AdministeredItem, P extends AdministeredItem> extends ItemController<I> {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
<<<<<<< HEAD
        super.getDisallowedProperties() + ['path', 'parent', 'owner']
=======
        ['class'] +
        ['id', 'version', 'dateCreated', 'lastUpdated' /*, 'domainType' */, 'createdBy', 'path', /*'breadcrumbTree',*/ 'parent', 'owner']
    }

    /**
     * Properties disallowed in a simple create request.
     */
    List<String> getDisallowedCreateProperties() {
        disallowedProperties
>>>>>>> 711f722 (More WIP.  Get BaseIntegrationSpec working.  Fix controllers and associated tests.  Try out running tests in Postgres test containers)
    }

    Class<I> itemClass

    AdministeredItemCacheableRepository<I> administeredItemRepository

    AdministeredItemCacheableRepository<P> parentItemRepository

    AdministeredItemContentRepository administeredItemContentRepository

    @Inject
    PathRepository pathRepository

    AdministeredItemController(Class<I> itemClass, AdministeredItemCacheableRepository<I> administeredItemRepository, AdministeredItemCacheableRepository<P> parentItemRepository, AdministeredItemContentRepository administeredItemContentRepository) {
        super(administeredItemRepository)
        this.itemClass = itemClass
        this.administeredItemRepository = administeredItemRepository
        this.parentItemRepository = parentItemRepository
        this.administeredItemContentRepository = administeredItemContentRepository
        this.administeredItemContentRepository.administeredItemRepository = administeredItemRepository
    }

    I show(UUID id) {
        I item = administeredItemRepository.findById(id)
        updateDerivedProperties(item)
        item
    }

    @Transactional
    I create(UUID parentId, @Body @NonNull I item) {
        cleanBody(item)

        P parent = parentItemRepository.readById(parentId)
        createEntity(parent, item)
    }

    protected I createEntity(@NonNull P parent, @NonNull I cleanItem) {
        updateCreationProperties(cleanItem)

        cleanItem.parent = parent

        updateDerivedProperties(cleanItem)
        administeredItemRepository.save(cleanItem)
    }

    I update(UUID id, @Body @NonNull I item) {
        cleanBody(item)
        I existing = administeredItemRepository.readById(id)
        updateEntity(existing, item)
    }


    protected I updateEntity(@NonNull I existing, @NonNull I cleanItem) {
        boolean hasChanged = updateProperties(existing, cleanItem)
        updateDerivedProperties(existing)

        if (hasChanged) {
            administeredItemRepository.update(existing)
        } else {
            existing
        }
    }

    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable I item) {
        I itemToDelete = (I) administeredItemContentRepository.readWithContentById(id)
        if (item?.version) itemToDelete.version = item.version
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
            updateDerivedProperties(it)
        }
        ListResponse.from(items)
    }

    protected I updateDerivedProperties(I item) {
        pathRepository.readParentItems(item)
        item.updatePath()

<<<<<<< HEAD
=======
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

>>>>>>> 711f722 (More WIP.  Get BaseIntegrationSpec working.  Fix controllers and associated tests.  Try out running tests in Postgres test containers)
        item
    }
}
