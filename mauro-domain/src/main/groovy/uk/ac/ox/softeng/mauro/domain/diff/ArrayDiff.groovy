package uk.ac.ox.softeng.mauro.domain.diff


import groovy.transform.CompileStatic

@CompileStatic
class ArrayDiff<K extends Diffable> extends FieldDiff<Collection<K>> {

    Collection<Object> created
    Collection<Object> deleted
    Collection<Object> modified

    ArrayDiff(Class<Collection<K>> targetArrayClass) {
        super(targetArrayClass)
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<K> createdObjects(Collection<Object> created) {
        this.created = created
        this
    }

    ArrayDiff<K> deletedObjects(Collection<Object> deleted) {
        this.deleted = deleted
        this
    }
 
}




