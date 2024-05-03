package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class CreationDiff<C extends Diffable> extends UniDirectionalDiff<C> {


    CreationDiff(Class<C> targetClass) {
        super(targetClass)
    }

    C getCreated() {
        println("**********cration diff; value: ${super.getValue()}")
        super.getValue() as C
    }

    String getCreatedIdentifier() {
        value.diffIdentifier
    }

    CreationDiff created(C object) {
        this.value = object
        this
    }
}