package org.maurodata.domain.model

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ImmutableOptions

/**
 * An abstracted reference to an Item or an AdministeredItem
 * that can be used to resolve the Item
 */
@CompileStatic
class ItemReference {

    Path pathToItem
    UUID itemId
    String itemDomainType

    Item theItem

    static ItemReference from(final Item item) {
        if (item instanceof AdministeredItem) {
            return new ItemReference(pathToItem: item.path, itemId: item.id, itemDomainType: correctDomainType(item.domainType), theItem: item)
        }
        return new ItemReference(pathToItem: null, itemId: item.id, itemDomainType: correctDomainType(item.domainType), theItem: item)
    }

    static ItemReference from(final Path path) {
        return new ItemReference(pathToItem: new Path(path.toString()), itemId: null, itemDomainType: null, theItem: null)
    }

    static ItemReference from(final UUID id, final String domainType) {
        return new ItemReference(pathToItem: null, itemId: id, itemDomainType: correctDomainType(domainType), theItem: null)
    }

    private static String correctDomainType(final String domainType) {
        if (domainType == null) {return null}
        if (domainType.endsWith("DTO")) {
            return domainType.substring(0, domainType.length() - 3)
        }
        return domainType
    }

    @Override
    String toString() {
        String referenceTo = "ItemReference to"
        if (pathToItem != null) {referenceTo += " ${pathToItem}"}
        if (itemId != null) {referenceTo += " ${itemId}"}
        if (itemDomainType != null) {referenceTo += " ${itemDomainType}"}
        referenceTo += " " + hashCode()
        return referenceTo
    }
}
