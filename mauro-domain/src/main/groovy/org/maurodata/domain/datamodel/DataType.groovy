package org.maurodata.domain.datamodel

import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.Path

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Transient
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

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
@MappedEntity(schema = 'datamodel', value = 'data_type')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataType extends ModelItem<DataModel> implements DiffableItem<DataType>, ItemReferencer {

    enum DataTypeKind {
        PRIMITIVE_TYPE('PrimitiveType'),
        ENUMERATION_TYPE('EnumerationType'),
        REFERENCE_TYPE('ReferenceType'),
        MODEL_TYPE('ModelDataType')

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

    @Nullable
    @ManyToOne
    @MappedProperty('reference_class_id')
    @JoinColumn(name = 'reference_class_id')
    DataClass referenceClass

    @Nullable
    @MappedProperty('model_resource_domain_type')
    String modelResourceDomainType

    @Nullable
    @MappedProperty('model_resource_id')
    UUID modelResourceId

    @Override
    String getDomainType() {
        dataTypeKind?.toString() ?:DataType.class.simpleName
    }

    @Override
    void setDomainType(String domainType) {
        super.setDomainType(domainType)
        dataTypeKind = DataTypeKind.fromString(domainType)
    }

    void setDomainType(DataTypeKind domainType) {
        super.setDomainType(domainType.stringValue)
        dataTypeKind = domainType
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

    @Transient
    @JsonIgnore
    boolean isReferenceType() {
        referenceClass && this.referenceClass.id
    }


    @Transient
    @JsonIgnore
    boolean isModelType() {
        modelResourceDomainType && modelResourceId
    }
    @Transient
    @JsonIgnore
    boolean isEnumerationType() {
        this.dataTypeKind == DataTypeKind.ENUMERATION_TYPE
    }

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, getDiffIdentifier(), label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (dataModel != null) {return "${dataModel.getDiffIdentifier()}|${getPathNodeString()}"}
        return "${getPathNodeString()}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<DataType> diff(DataType other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<DataType> base = DiffBuilder.objectDiff(DataType)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base
    }

    @Transient
    @Deprecated
    @JsonProperty('model')
    UUID getModelId() {
        dataModel?.id ?: owner?.id // backwards compatibility
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

    String domainType(DataTypeKind dataTypeKind) {
        setDomainType(dataTypeKind)
        this.domainType
    }

    String units(String units) {
        this.units = units
        this.units
    }

    EnumerationValue enumerationValue(EnumerationValue enumerationValue) {
        enumerationValues.add(enumerationValue)
        enumerationValue.enumerationType = this
        dataModel.enumerationValues.add(enumerationValue)
        enumerationValue.dataModel = dataModel
        enumerationValue
    }

    EnumerationValue enumerationValue(Map args, @DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        EnumerationValue enumValue = EnumerationValue.build(args, closure)
        enumerationValue enumValue
    }

    EnumerationValue enumerationValue(@DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        enumerationValue [:], closure
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> getItemReferences() {
        List<ItemReference> pathsBeingReferenced = []
        if (referenceClass != null) {
            pathsBeingReferenced << ItemReference.from(referenceClass)
        }
        if (modelResourceId != null) {
            pathsBeingReferenced << ItemReference.from(modelResourceId,modelResourceDomainType)
        }
        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferences(Map<UUID, ItemReference> replacements) {
        if (referenceClass != null) {
            ItemReference replacementItemReference = replacements.get(referenceClass.id)
            if (replacementItemReference != null) {referenceClass = (DataClass) replacementItemReference.theItem}
        }
        if (modelResourceId != null) {
            ItemReference replacementItemReference = replacements.get(modelResourceId)
            if (replacementItemReference != null) {
                modelResourceId = replacementItemReference.itemId
            }
        }
    }
}
