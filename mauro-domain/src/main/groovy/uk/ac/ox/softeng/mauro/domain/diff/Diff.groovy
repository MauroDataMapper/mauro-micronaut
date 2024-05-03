package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic


@CompileStatic
abstract class Diff<T> {

    T value

    @JsonIgnore
    Class<T> targetClass

    protected Diff(Class<T> targetClass) {
        this.targetClass = targetClass

    }

    abstract Integer getNumberOfDiffs()

    String getDiffType() {
        getClass().simpleName
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Diff<T> diff = (Diff<T>) o
        value == diff.value
    }

    boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

    @Override
    int hashCode() {
        (value != null ? value.hashCode() : 0)
    }
}


