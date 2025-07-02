package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class RuleDiff extends CollectionDiff{

    String name
    String description

    RuleDiff(UUID id, String name, String description, String diffIdentifier) {
        super(id,diffIdentifier)
        this.name = name
        this.description = description
    }
}
