package org.maurodata.domain.diff

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable

@CompileStatic
class BaseCollectionDiff<F> extends CollectionDiff {

    @Nullable
    String label

    BaseCollectionDiff(UUID id, String diffIdentifier) {
        super(id, diffIdentifier)
    }

    BaseCollectionDiff(UUID id, String diffIdentifier, @Nullable String label) {
        super(id, diffIdentifier)
        this.label = label
    }

    String toString(){
        return "<BaseCollectionDiff> "+label+" <super>"+super.toString()+"</super></BaseCollectionDiff>"
    }
}
