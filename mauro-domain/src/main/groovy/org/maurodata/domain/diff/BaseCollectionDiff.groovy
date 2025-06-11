package org.maurodata.domain.diff

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable

@CompileStatic
class BaseCollectionDiff extends CollectionDiff {

    @Nullable
    String label

    BaseCollectionDiff(UUID id) {
        super(id)
    }

    BaseCollectionDiff(UUID id, @Nullable String label) {
        super(id)
        this.label = label
    }
}
