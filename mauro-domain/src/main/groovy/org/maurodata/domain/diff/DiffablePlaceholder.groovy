package org.maurodata.domain.diff

import groovy.transform.CompileStatic

/**
 * Really a placeholder for an object with the DiffableItem trait
 * that returns the given diffIdentifier
 */
@CompileStatic
class DiffablePlaceholder implements DiffableItem{

    String diffIdentifier

    @Override
    CollectionDiff fromItem() {
        return null
    }

    @Override
    ObjectDiff diff(DiffableItem other, String lhsPathRoot, String rhsPathRoot) {
        return null
    }

    String toString()
    {
        return "DiffablePlaceholder: "+diffIdentifier
    }
}
