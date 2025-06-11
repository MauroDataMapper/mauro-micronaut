package org.maurodata.domain.diff

/**
 * Really a placeholder for an object with the DiffableItem trait
 * that returns the given diffIdentifier
 */
class DiffablePlaceholder implements DiffableItem{

    String diffIdentifier

    @Override
    CollectionDiff fromItem() {
        return null
    }

    @Override
    ObjectDiff diff(DiffableItem other) {
        return null
    }

    String toString()
    {
        return "DiffablePlaceholder: "+diffIdentifier
    }
}
