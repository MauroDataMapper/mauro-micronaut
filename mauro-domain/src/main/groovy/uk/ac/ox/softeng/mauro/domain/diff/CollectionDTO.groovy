package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CollectionDTO {
    Map<String, Collection<DiffableItem>> fieldCollections = [:]

      void addField(String field, Collection<DiffableItem> collection) {
        fieldCollections[field] = collection ?: []
    }

}
