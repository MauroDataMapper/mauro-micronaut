package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class BaseCollectionDiff extends CollectionDiff {

    String label


    BaseCollectionDiff(UUID id, String label) {
        super(id)
        this.label = label
    }

}
