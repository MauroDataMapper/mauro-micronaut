package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class DeletionDiff<C extends Diffable> extends UniDirectionalDiff<C> {


    DeletionDiff(Class<C> targetClass) {
        super(targetClass)
    }

    C getDeleted() {
        super.getValue() as C
    }

    String getDeletedIdentifier() {
        value.diffIdentifier
    }

    DeletionDiff deleted(C object) {
        this.value = object
        this
    }
}