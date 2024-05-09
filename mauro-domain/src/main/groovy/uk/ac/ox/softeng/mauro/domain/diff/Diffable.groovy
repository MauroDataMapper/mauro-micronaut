package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
trait Diffable<T extends Diffable> {

    abstract ObjectDiff diff(T other)

    abstract ObjectDiff diff(T that, String context)

    String getDiffIdentifier() {
        getPathIdentifier()
    }

    abstract String getPathIdentifier()
}