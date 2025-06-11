package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CollectionDTO {
    Map<String, Collection<DiffableItem>> fieldCollections = [:]

    void addField(String field, Collection<DiffableItem> collection) {
        if (collection.size() > 0) {
            fieldCollections.putIfAbsent(field, collection.unique())
        }
    }

}
