package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
abstract class CollectionDiff {
    UUID id

    CollectionDiff(UUID id) {
        this.id = id
    }
}
