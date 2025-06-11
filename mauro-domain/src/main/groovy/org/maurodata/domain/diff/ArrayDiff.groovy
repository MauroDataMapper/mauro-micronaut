package org.maurodata.domain.diff


import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic

@CompileStatic
class ArrayDiff<K> extends FieldDiff<Collection<K>> {

    Collection<K> created
    Collection<K> deleted
    Collection<ObjectDiff> modified

    ArrayDiff() {
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<K> createdObjects(Collection<K> createdItems) {
        createdItems.each{
            DiffableItem diffableItem = (DiffableItem) (it)
            CollectionDiff collectionDiff = diffableItem.fromItem()
            if( !this.created.contains(collectionDiff)) {
                this.created.add(collectionDiff)
            }
        }
        this
    }

    ArrayDiff<K> deletedObjects(Collection<K> deletedItems) {
        deletedItems.each{
            DiffableItem diffableItem = (DiffableItem) (it)
            CollectionDiff collectionDiff = diffableItem.fromItem()
            if (! this.deleted.contains(collectionDiff)) {
                this.deleted.add(diffableItem.fromItem())
            }
        }
        this
    }

    ArrayDiff<K> modifiedObjects(Collection<ObjectDiff> modifiedItems) {
        this.modified = modifiedItems
        this
    }

    @Override
    @JsonProperty('count')
    Integer getNumberOfDiffs() {
        created.size() + deleted.size() + ((modified.sum {it.getNumberOfDiffs() } ?: 0) as Integer)
    }
}




