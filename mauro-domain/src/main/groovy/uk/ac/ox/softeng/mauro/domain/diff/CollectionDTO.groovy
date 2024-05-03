package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CollectionDTO {
    Map<String, Collection<Object>> fieldCollections = [:]

    def <D extends Diffable> Collection<Object> getCollection(String field, Class<D> diffableClass) {
        fieldCollections[field] ?: []
    }
    void addField(String field, Collection<Object> collection) {
        // Cannot have a null value in a CHM so add empty set if no collection
        fieldCollections[field] = collection ?: []
    }
    CollectionDTO setFieldCollections(Map<String, Collection> fieldCollections){
        this.fieldCollections = fieldCollections as Map<String, Collection<Object>>
        this
    }

}
