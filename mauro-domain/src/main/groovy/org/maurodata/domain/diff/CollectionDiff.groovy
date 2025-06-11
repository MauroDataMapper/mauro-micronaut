package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
abstract class CollectionDiff {
    UUID id

    CollectionDiff(UUID id) {
        this.id = id
    }
}
