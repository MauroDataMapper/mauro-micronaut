package org.maurodata.domain.diff

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic

@CompileStatic
abstract class CollectionDiff {
    UUID id

    @JsonIgnore
    String diffIdentifier

    CollectionDiff(UUID id, String diffIdentifier) {
        this.id = id
        this.diffIdentifier = diffIdentifier
    }

    String toString(){
        return "<CollectionDiff> "+id?.toString()+" "+diffIdentifier+" </CollectionDiff>"
    }
}
