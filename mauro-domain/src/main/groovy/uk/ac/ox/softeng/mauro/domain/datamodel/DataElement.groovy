package uk.ac.ox.softeng.mauro.domain.datamodel

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
import uk.ac.ox.softeng.mauro.domain.diff.*
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
@AutoClone(excludes = ['dataModel', 'dataType'])
@Introspected
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@MappedEntity(schema = 'datamodel', value = 'data_element')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataElement extends ModelItem<DataClass> implements DiffableItem<DataElement> {

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
    DataClass getParent() {
        this.dataClass
    }

    @Override
    @Transient
    @JsonIgnore
    Model getOwner() {
        dataModel ?: super.getOwner()
    }

    @Transient
    @Deprecated
    @JsonProperty('model')
    UUID getModelId() {
        dataModel?.id ?: owner?.id // backwards compatibility
    }

    @Transient
    @Deprecated
    @JsonProperty('dataClass')
    UUID getDataClassId() {
        dataClass?.id // backwards compatibility
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem dataClass) {
        this.dataClass = (DataClass) dataClass
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'de'
    }

    @Override
    @Transient
    @JsonIgnore
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, label)
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        "${dataClass?.getDiffIdentifier()}/$this.pathIdentifier}"
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<DataElement> diff(DataElement other) {
        ObjectDiff<DataElement> base = DiffBuilder.objectDiff(DataElement)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        base.appendString(DiffBuilder.DATA_TYPE_PATH, this.dataType.path?.toString(), other.dataType.path?.toString(), this, other)
        base.appendField(DiffBuilder.MIN_MULTIPILICITY, this.minMultiplicity, other.minMultiplicity, this, other)
        base.appendField(DiffBuilder.MAX_MULTIPILICITY, this.maxMultiplicity, other.maxMultiplicity, this, other)
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
