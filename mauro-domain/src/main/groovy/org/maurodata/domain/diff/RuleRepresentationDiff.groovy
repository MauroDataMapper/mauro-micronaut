package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class RuleRepresentationDiff extends CollectionDiff {

    String language
    String representation

    RuleRepresentationDiff(UUID id, String language, String representation) {
        super(id)
        this.language = language
        this.representation = representation
    }


}
