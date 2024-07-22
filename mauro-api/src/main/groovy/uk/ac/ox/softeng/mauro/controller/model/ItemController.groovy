package uk.ac.ox.softeng.mauro.controller.model

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class ItemController<I extends Item> implements AdministeredItemReader {

    @Inject
    AccessControlService accessControlService

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        ['class', 'id', 'dateCreated', 'lastUpdated', 'createdBy']
    }

    /**
     * Properties disallowed in a simple create request.
     */
    List<String> getDisallowedCreateProperties() {
        disallowedProperties
    }

    ItemRepository<I> itemRepository

    ItemController(ItemRepository<I> itemRepository) {
        this.itemRepository = itemRepository
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

    protected Item updateCreationProperties(Item item) {
        item.updateCreationProperties()
        item.catalogueUser = accessControlService.getUser()
        item
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
}
