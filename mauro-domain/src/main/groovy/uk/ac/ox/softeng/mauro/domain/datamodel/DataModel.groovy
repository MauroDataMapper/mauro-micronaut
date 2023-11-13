package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

/**
 * A DataModel describes a data asset, or a data standard
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataModel extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataType> dataTypes = []

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dm'
    }

    @Override
    @Transient
    @JsonIgnore
    Collection<AdministeredItem> getAllContents() {
        List<AdministeredItem> items = []
        dataTypes?.each {it.dataModel = this}
        if (dataTypes) items.addAll(dataTypes)
        items
    }

    @Override
    DataModel clone() {
        DataModel cloned = (DataModel) super.clone()
        cloned.dataTypes = dataTypes.collect {it.clone().tap {it.parent = cloned}}

        cloned
    }

    /****
     * Methods for building a tree-like DSL
     */

    static DataModel build(
            Map args,
            @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new DataModel(args).tap(closure)
    }

    static DataModel build(
            @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    DataType dataType(DataType dataType) {
        this.dataTypes.add(dataType)
        dataType.dataModel = this
        dataType
    }

    DataType dataType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dt = DataType.build(args, closure)
        dt.dataModel = this
        this.dataTypes.add(dt)
        dt
    }

    DataType dataType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        dataType [:], closure
    }

}
