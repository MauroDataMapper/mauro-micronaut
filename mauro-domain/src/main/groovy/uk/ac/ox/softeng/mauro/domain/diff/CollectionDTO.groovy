package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CollectionDTO {
    Map<String, Collection<DiffableItem>> fieldCollections = [:]

    def <D extends Diffable> Collection<DiffableItem> getCollection(String field, Class<D> diffableClass) {
        fieldCollections[field] ?: []
    }
    void addField(String field, Collection<DiffableItem> collection) {
        // Cannot have a null value in a CHM so add empty set if no collection
        fieldCollections[field] = collection ?: []
    }
    CollectionDTO setFieldCollections(Map<String, Collection> fieldCollections){
        this.fieldCollections = fieldCollections as Map<String, Collection<DiffableItem>> as Map<String, Collection<DiffableItem>>
        this
    }

}
