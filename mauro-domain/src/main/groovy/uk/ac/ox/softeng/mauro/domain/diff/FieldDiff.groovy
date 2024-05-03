package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
class FieldDiff<F> extends BiDirectionalDiff<F> {

    @JsonIgnore
    String name

   // F left
   // F right
    @JsonCreator
    FieldDiff(Class<F> targetClass) {
        super(targetClass)
    }

    FieldDiff(Class<F> targetClass, String name, F left, F right) {
        super(targetClass)
        this.name = name
        super.left = left
        super.right = right
    }

    FieldDiff<F> name(String name) {
        this.name = name
        this
    }

    FieldDiff<F> leftHandSide(F lhs) {
        println("********* FieldDiff: lhs: Field LHS: $lhs")
        (this.left = lhs) as FieldDiff<F>
        this
    }


    FieldDiff<F> rightHandSide(F rhs) {
        (this.right = rhs) as FieldDiff<F>
        this
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }
}
