package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
abstract class UniDirectionalDiff<U extends Diffable> extends Diff<U> {

    protected UniDirectionalDiff(Class<U> targetClass) {
        super(targetClass)
    }

    String getValueIdentifier() {
        value.diffIdentifier
    }

    @Override
    String toString() {
        value.toString()
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }


}
