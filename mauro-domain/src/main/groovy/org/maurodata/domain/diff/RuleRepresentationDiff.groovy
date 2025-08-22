package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class RuleRepresentationDiff extends CollectionDiff {

    String language
    String representation

    RuleRepresentationDiff(UUID id, String language, String representation, String diffIdentifier) {
        super(id,diffIdentifier)
        this.language = language
        this.representation = representation
    }


}
