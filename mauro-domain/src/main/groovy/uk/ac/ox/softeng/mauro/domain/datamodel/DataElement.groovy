package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull

/**
 * A datatype describes the range of values that a column or field in a dataset may take.  It may be one of the following kinds:
 * - a primitive type, such as a string or integer;
 * - a set of enumerated values;
 * - a reference type pointing at another class in the model;
 * - a model reference type pointing at another model such as a terminology or reference data model
 * <p>
 * This was previously modelled using inheritance, with a super class `DataType` and subclasses for `PrimitiveType`, `EnumerationType`, etc.
 * Due to the limitations of the micronaut data support for inheritance, this is currently modelled as a single class, but might be changed in future.
 *
 */

@CompileStatic
@AutoClone(excludes = ['dataModel'])
@Introspected
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@MappedEntity(schema = 'datamodel', value = 'data_element')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataElement extends ModelItem<DataModel> {

    @Nullable
    Integer minMultiplicity

    @Nullable
    Integer maxMultiplicity

    @JsonIgnore
    @Transient
    DataModel dataModel

    @JsonIgnore
    @NotNull
    DataClass dataClass

    @NotNull
    DataType dataType

    @Override
    @Transient
    @JsonIgnore
    DataModel getParent() {
        dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem dataModel) {
        this.dataModel = (DataModel) dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'de'
    }

    /****
     * Methods for building a tree-like DSL
     */

    static DataElement build(
        Map args,
        @DelegatesTo(value = DataElement, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new DataElement(args).tap(closure)
    }

    static DataElement build(@DelegatesTo(value = DataElement, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    DataType dataType(DataType dataType) {
        this.dataType = dataType
        dataType.dataModel = this.dataModel
        this.dataModel.dataTypes.add(dataType)
        dataType
    }

    DataType dataType(String dataTypeLabel) {
        DataType dataType = dataModel.dataTypes.find {it.label == dataTypeLabel}
        this.dataType = dataType
        dataType
    }

    DataType primitiveType(DataType primitiveType) {
        this.dataType = primitiveType
        this.dataModel.dataTypes.add(primitiveType)
        primitiveType.dataModel = this.dataModel
        primitiveType.setDomainType(DataType.DataTypeKind.PRIMITIVE_TYPE)
        primitiveType
    }

    DataType primitiveType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dt = DataType.build(args, closure)
        this.dataType = dt
        this.dataModel.dataTypes.add(dt)
        dt.dataModel = this.dataModel
        dt.setDomainType(DataType.DataTypeKind.PRIMITIVE_TYPE)
        dt
    }

    DataType primitiveType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        primitiveType [:], closure
    }

    DataType enumerationType(DataType enumerationType) {
        this.dataType = enumerationType
        this.dataModel.dataTypes.add(enumerationType)
        enumerationType
    }

    DataType enumerationType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dt = DataType.build(args + [dataModel: this.dataModel, dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE], closure)
        enumerationType(dt)
    }

    DataType enumerationType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        enumerationType [:], closure
    }

    DataType dataType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataType dataType = DataType.build(args + [dataModel: this.dataModel], closure)
        this.dataType = dataType
        this.dataModel.dataTypes.add(dataType)
        dataType
    }

    DataType dataType(@DelegatesTo(value = DataElement, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        dataType [:], closure
    }



    Integer maxMultiplicity(Integer maxMultiplicity) {
        this.maxMultiplicity = maxMultiplicity
        this.maxMultiplicity
    }

    Integer minMultiplicity(Integer minMultiplicity) {
        this.minMultiplicity = minMultiplicity
        this.minMultiplicity
    }

}
