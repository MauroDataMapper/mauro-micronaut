package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.Path
@CompileStatic
trait Diffable<T extends Diffable> {

    abstract ObjectDiff diff(T other)

    abstract ObjectDiff diff(T that, String context)

    String getDiffIdentifier() {
        getDiffIdentifier(null)
    }
    String getDiffIdentifier(String context) {
        null
    }
}