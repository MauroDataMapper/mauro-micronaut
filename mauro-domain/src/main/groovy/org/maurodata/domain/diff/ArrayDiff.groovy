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

    @Override
    String toString(){
        String description="\n<ArrayDiff> <super>"+super.toString()+"</super> "

        if (created.size() > 0) {
            description += "<created> "
            created.forEach {
                description += it.getClass().simpleName+":"+it.toString() + ", "
            }
            description += " </created>"
        }

        if (deleted.size() > 0) {
            description += "<deleted> "
            deleted.forEach {
                description += it.toString() + ", "
            }
            description += " </deleted>"
        }

        if (modified.size() > 0) {
            description += "<modified> "
            modified.forEach {
                description += it.toString() + ", "
            }
            description += " </modified>"
        }

        description+="</ArrayDiff>"

        return description
    }
}




