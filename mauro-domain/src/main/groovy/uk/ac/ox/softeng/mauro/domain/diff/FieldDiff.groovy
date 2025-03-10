package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

@CompileStatic
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes([
    @JsonSubTypes.Type(value = FieldDiff.class, name = "fieldDiff"),
    @JsonSubTypes.Type(value = ArrayDiff.class, name = "arrayDiff")])
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

    @JsonIgnore
    Integer getNumberOfDiffs() {
      1
    }
}
