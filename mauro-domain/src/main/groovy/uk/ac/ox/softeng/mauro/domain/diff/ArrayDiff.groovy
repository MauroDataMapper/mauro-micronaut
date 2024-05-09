package uk.ac.ox.softeng.mauro.domain.diff


import groovy.transform.CompileStatic

@CompileStatic
class ArrayDiff<K> extends FieldDiff<Collection<K>> {

    Collection<K> created
    Collection<K> deleted
    Collection<K> modified

    ArrayDiff() {
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<K> createdObjects(Collection<K> created) {
        this.created = created
        this
    }

    ArrayDiff<K> deletedObjects(Collection<K> deleted) {
        this.deleted = deleted
        this
    }

    @Override
    Integer getNumberOfDiffs() {
        //  created.size() + deleted.size() + ((modified.sum { it.getNumberOfDiffs() } ?: 0) as Integer)
        created.size() + deleted.size()
    }

}




