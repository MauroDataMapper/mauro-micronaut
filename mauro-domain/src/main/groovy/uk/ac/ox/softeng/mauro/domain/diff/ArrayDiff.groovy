package uk.ac.ox.softeng.mauro.domain.diff


import groovy.transform.CompileStatic

@CompileStatic
class ArrayDiff<K extends Diffable> extends FieldDiff<Collection<K>> {

    Collection<CollectionDiff> created
    Collection<CollectionDiff> deleted
    Collection<CollectionDiff> modified

    ArrayDiff() {
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<K> createdObjects(Collection<CollectionDiff> created) {
        this.created = created
        this
    }

    ArrayDiff<K> deletedObjects(Collection<CollectionDiff> deleted) {
        this.deleted = deleted
        this
    }

}




