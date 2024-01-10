package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
@MappedEntity(schema = 'datamodel')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataModel extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataType> dataTypes = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataClass> dataClasses = []


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dm'
    }

    @Override
    @Transient
    @JsonIgnore
    List<List<? extends ModelItem<DataModel>>> getAllAssociations() {
        [dataTypes, dataClasses]
    }


    @Transient
    @JsonIgnore
    List<DataClass> getChildDataClasses() {
        dataClasses.findAll{ !it.parentDataClass}
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

    DataType primitiveType(DataType primitiveType) {
        this.dataTypes.add(primitiveType)
        primitiveType.dataModel = this
        primitiveType.dataTypeKind = DataType.DataTypeKind.PRIMITIVE_TYPE
        primitiveType
    }

    DataType primitiveType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dt = DataType.build(args, closure)
        dt.dataModel = this
        this.dataTypes.add(dt)
        dt.dataTypeKind = DataType.DataTypeKind.PRIMITIVE_TYPE
        dt
    }

    DataType primitiveType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        primitiveType [:], closure
    }

    DataType enumerationType(DataType enumerationType) {
        this.dataTypes.add(enumerationType)
        enumerationType.dataModel = this
        enumerationType.dataTypeKind = DataType.DataTypeKind.ENUMERATION_TYPE
        enumerationType
    }

    DataType enumerationType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dt = DataType.build(args, closure)
        dt.dataModel = this
        this.dataTypes.add(dt)
        dt.dataTypeKind = DataType.DataTypeKind.ENUMERATION_TYPE
        dt
    }

    DataType enumerationType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        enumerationType [:], closure
    }


    DataClass dataClass(DataClass dataClass) {
        this.dataClasses.add(dataClass)
        dataClass.dataModel = this
        dataClass
    }

    DataClass dataClass(Map args, @DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataClass dataClass = DataClass.build(args + [dataModel: this], closure)
        this.dataClasses.add(dataClass)
        dataClass
    }

    DataClass dataClass(@DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        dataClass [:], closure
    }

}
