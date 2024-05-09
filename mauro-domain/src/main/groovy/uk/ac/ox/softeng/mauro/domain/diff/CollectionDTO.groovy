package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CollectionDTO {
    Map<String, Collection<DiffableItem>> fieldCollections = [:]

      void addField(String field, Collection<DiffableItem> collection) {
        // Cannot have a null value in a CHM so add empty set if no collection
        fieldCollections[field] = collection ?: []
    }

}
