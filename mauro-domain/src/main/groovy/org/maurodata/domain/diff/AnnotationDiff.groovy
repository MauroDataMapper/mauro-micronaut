package org.maurodata.domain.diff

import groovy.transform.CompileStatic
import org.maurodata.domain.facet.Annotation

@CompileStatic
class AnnotationDiff extends CollectionDiff {

    String label

    String description

    Collection<Annotation> childAnnotations

    AnnotationDiff(UUID id, String label, String description, Collection<Annotation> childAnnotations, String diffIdentifier) {
        super(id,diffIdentifier)
        this.label = label
        this.description = description
        this.childAnnotations = childAnnotations
    }

}
