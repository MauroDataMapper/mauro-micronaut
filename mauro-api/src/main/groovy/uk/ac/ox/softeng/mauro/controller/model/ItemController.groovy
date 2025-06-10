package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class ItemController<I extends Item> implements AdministeredItemReader {

    @Inject
    AccessControlService accessControlService

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        ['class_', 'class', 'id', 'dateCreated', 'lastUpdated', 'createdBy', 'versionable', 'domainType', 'version']
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

    I cleanBody(I item, boolean strictProperties = true) {
        I defaultItem = (I) item.class.getDeclaredConstructor().newInstance()

        if (strictProperties) {
            // Disallowed properties cannot be set by user request
            disallowedCreateProperties.each {String key ->
                if (defaultItem.hasProperty(key)?.properties?.setter && item[key] != defaultItem[key]) {
                    throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Property $key cannot be set directly")
                }
            }
        } else {
            disallowedCreateProperties.each {String key ->
                if (defaultItem.hasProperty(key)?.properties?.setter && item[key] != null && item[key] != defaultItem[key]) {
                    item[key] = null
                }
            }
        }

        // Collection properties cannot be set in user requests as these might be used in services
        defaultItem.properties.each {
            String key = it.key
            if (defaultItem.hasProperty(key).properties.setter
                && (isNotClassifiersCollection(key) && it.value instanceof Collection)
                || it.value instanceof Map) {
                if (item[key]) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Collection or Map $key cannot be set directly")
                item[key] = null
            }
        }

        item
    }

    protected boolean isNotClassifiersCollection(String key) {
        !key.toLowerCase().contains(Classifier.class.simpleName.toLowerCase())
    }

    protected Item updateCreationProperties(Item item) {
        item.updateCreationProperties()
        item.catalogueUser = accessControlService.getUser()
        item
    }

    boolean updateProperties(I existing, I cleanItem) {
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
