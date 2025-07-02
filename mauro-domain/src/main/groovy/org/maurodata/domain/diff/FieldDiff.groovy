package org.maurodata.domain.diff

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

    @JsonIgnore
    DiffableItem lhsDiffableItem
    @JsonIgnore
    DiffableItem rhsDiffableItem

    @JsonCreator
    FieldDiff(){}

    FieldDiff( String name, F left, F right, DiffableItem lhsDiffableItem, DiffableItem rhsDiffableItem) {
        this.name = name
        this.left = left
        this.right = right
        this.lhsDiffableItem = lhsDiffableItem
        this.rhsDiffableItem = rhsDiffableItem
    }

    @JsonIgnore
    Integer getNumberOfDiffs() {
      1
    }

    String toString()
    {
        return "\n<FieldDiff> "+name+"\n left="+left+"\n right="+right+"\n lhsDiffableItem="+lhsDiffableItem+"\n rhsDiffableItem="+rhsDiffableItem+"</FieldDiff>"
    }

}
