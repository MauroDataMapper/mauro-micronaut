package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.MappedSuperclass
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
@MappedEntity('data_type')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataType extends ModelItem<DataModel> {

    enum DataTypeKind {
        PRIMITIVE_TYPE('PrimitiveType'),
        ENUMERATION_TYPE('EnumerationType'),
        REFERENCE_TYPE('ReferenceType'),
        MODEL_TYPE('ModelType')

        public final String stringValue

        private DataTypeKind(String stringValue) {
            this.stringValue = stringValue
        }

        @Override
        String toString() {
            stringValue
        }

        static DataTypeKind fromString(String text) {
            values().find {it -> it.stringValue.equalsIgnoreCase(text) }
        }
    }

    // Remove the transient annotation here
    String domainType

    @JsonIgnore
    DataModel dataModel

    @Transient
    @JsonIgnore
    DataTypeKind dataTypeKind

    @Override
    String getDomainType() {
        dataTypeKind?.toString()
    }

    @Override
    void setDomainType(String domainType) {
        super.setDomainType(domainType)
        dataTypeKind = DataTypeKind.fromString(domainType)
    }

    @Override
    @Transient
    @JsonIgnore
    DataModel getParent() {
        dataModel
    }

    // For Primitive Types only
    @Nullable
    String units

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'enumerationType')
    List<EnumerationValue> enumerationValues = []


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
        'dt'
    }

    /****
     * Methods for building a tree-like DSL
     */

    static DataType build(
        Map args,
        @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new DataType(args).tap(closure)
    }

    static DataType build(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    String units(String units) {
        this.units = units
        this.units
    }

    EnumerationValue enumerationValue(EnumerationValue enumerationValue) {
        this.enumerationValues.add(enumerationValue)
        enumerationValue.enumerationType = this
        enumerationValue
    }

    EnumerationValue enumerationValue(Map args, @DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        EnumerationValue enumerationValue = EnumerationValue.build(args, closure)
        enumerationValue.enumerationType = this
        this.enumerationValues.add(enumerationValue)
        enumerationValue
    }

    EnumerationValue enumerationValue(@DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        enumerationValue [:], closure
    }


}
