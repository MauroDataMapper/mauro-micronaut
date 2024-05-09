package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class AnnotationDiff extends CollectionDiff {

    String label


    AnnotationDiff(UUID id, String label) {
        super(id)
        this.label = label
    }

}
