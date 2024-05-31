package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class AnnotationDiff extends BaseCollectionDiff {


    AnnotationDiff(UUID id, String label) {
        super(id, label)
    }

}
