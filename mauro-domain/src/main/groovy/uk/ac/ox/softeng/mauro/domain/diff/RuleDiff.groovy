package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class RuleDiff extends CollectionDiff{

    String name
    String description

    RuleDiff(UUID id, String name, String description) {
        super(id)
        this.name = name
        this.description = description
    }
}
