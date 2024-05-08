package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
class FieldDiff<F> {

    String name
    F left
    F right

    @JsonCreator
    FieldDiff(){}

    FieldDiff( String name, F left, F right) {
        this.name = name
        this.left = left
        this.right = right
    }


}
