package org.maurodata.domain.datamodel

import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Transient
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
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
@MappedEntity(schema = 'datamodel', value = 'data_class')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataClass extends ModelItem<DataModel> implements DiffableItem<DataClass>, ItemReferencer {

    @Nullable
    Integer minMultiplicity

    @Nullable
    Integer maxMultiplicity


    @JsonIgnore
    DataModel dataModel

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'parentDataClass')
    List<DataClass> dataClasses = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataClass')
    List<DataElement> dataElements = []

    @Relation(value = Relation.Kind.MANY_TO_MANY, mappedBy = 'extendedBy')
    @JoinTable(
        name = "join_dataclass_to_extended_data_class",
        joinColumns = @JoinColumn(name = "dataclass_id"),
        inverseJoinColumns = @JoinColumn(name = "extended_dataclass_id"))
    List<DataClass> extendsDataClasses = []

    @Relation(value = Relation.Kind.MANY_TO_MANY, mappedBy = 'extendsDataClasses')
    @JsonIgnore
    List<DataClass> extendedBy = []

    @Nullable
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'referenceClass')
    @JsonIgnore
    List<DataType> referenceTypes = []

    @JsonIgnore
    @Nullable
    DataClass parentDataClass

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        parentDataClass ?: dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    Model getOwner() {
        parentDataClass?.owner ?: dataModel ?: super.getOwner()
    }

    @Transient
    @Deprecated
    @JsonProperty('model')
    UUID getModelId() {
        dataModel?.id // backwards compatibility
    }

    @Transient
    @Deprecated
    @JsonProperty('parentDataClass')
    UUID getParentDataClassId() {
        parentDataClass?.id // backwards compatibility
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        if (parent instanceof DataClass) {
            this.parentDataClass = parent
            this.dataModel = parentDataClass.dataModel
        } else {
            this.dataModel = (DataModel) parent
        }
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dc'
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
        if (parentDataClass != null) {
            return "${parentDataClass.diffIdentifier}|${getPathNodeString()}"
        }
        if (dataModel != null) {
            return "${dataModel.getDiffIdentifier()}|${getPathNodeString()}"
        }
        return "${getPathNodeString()}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<DataClass> diff(DataClass other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<DataClass> base = DiffBuilder.objectDiff(DataClass)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        base.appendField(DiffBuilder.MIN_MULTIPILICITY, this.minMultiplicity, other.minMultiplicity, this, other)
        base.appendField(DiffBuilder.MAX_MULTIPILICITY, this.maxMultiplicity, other.maxMultiplicity, this, other)
        if (!DiffBuilder.isNullOrEmpty(this.dataClasses as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.dataClasses as Collection<Object>)) {
            base.appendCollection(DiffBuilder.DATA_CLASSES, this.dataClasses as Collection<DiffableItem>, other.dataClasses as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        if (!DiffBuilder.isNullOrEmpty(this.dataElements as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.dataElements as Collection<Object>)) {
            base.appendCollection(DiffBuilder.DATA_ELEMENTS, this.dataElements as Collection<DiffableItem>, other.dataElements as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        base
    }

    /****
     * Methods for building a tree-like DSL
     */

    static DataClass build(
        Map args,
        @DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new DataClass(args).tap(closure)
    }

    static DataClass build(@DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    DataClass dataClass(DataClass dataClass) {
        this.dataClasses.add(dataClass)
        dataClass.parentDataClass = this
        dataClass.dataModel = this.dataModel
        this.dataModel.allDataClasses.add(dataClass)
        dataClass
    }

    DataClass dataClass(Map args, @DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataClass dataClass = build(args + [dataModel: this.dataModel], closure)
        this.dataClasses.add(dataClass)
        dataClass.parentDataClass = this
        dataModel.allDataClasses.add(dataClass)
        dataClass
    }

    DataClass dataClass(@DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        dataClass [:], closure
    }

    DataElement dataElement(DataElement dataElement) {
        this.dataElements.add(dataElement)
        dataElement.dataClass = this
        dataElement.dataModel = this.dataModel
        dataModel.dataElements.add(dataElement)
        dataElement
    }

    DataElement dataElement(Map args, @DelegatesTo(value = DataElement, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataElement dataElement1 = DataElement.build(args + [dataModel: this.dataModel], closure)
        dataElement dataElement1
    }

    DataElement dataElement(@DelegatesTo(value = DataElement, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        dataElement [:], closure
    }

    Integer maxMultiplicity(Integer maxMultiplicity) {
        this.maxMultiplicity = maxMultiplicity
        this.maxMultiplicity
    }

    Integer minMultiplicity(Integer minMultiplicity) {
        this.minMultiplicity = minMultiplicity
        this.minMultiplicity
    }

    DataClass extendsDataClass(String dataClassLabel) {
        DataClass foundDataClass = this.dataModel.dataClasses.findAll {it.label == dataClassLabel}.first()
        if (foundDataClass != null) {
            if (!this.extendsDataClasses.contains(foundDataClass)) {
                this.extendsDataClasses.add(foundDataClass)
            }
            if (!foundDataClass.extendedBy.contains(this)) {
                foundDataClass.extendedBy.add(this)
            }
        }
        return foundDataClass
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> getItemReferences() {
        List<ItemReference> pathsBeingReferenced = []
        if (dataClasses != null) {
            dataClasses.forEach {DataClass dataClass ->
                pathsBeingReferenced << ItemReference.from(dataClass)
            }
        }
        if (dataElements != null) {
            dataElements.forEach {DataElement dataElement ->
                pathsBeingReferenced << ItemReference.from(dataElement)
            }
        }
        if (referenceTypes != null) {
            referenceTypes.forEach {DataType dataType ->
                pathsBeingReferenced << ItemReference.from(dataType)
            }
        }
        if (extendsDataClasses != null) {
            extendsDataClasses.forEach {DataClass dataClass ->
                pathsBeingReferenced << ItemReference.from(dataClass)
            }
        }
        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferences(Map<UUID, ItemReference> replacements) {
        if (dataClasses != null) {
            final List<DataClass> replacement = []
            dataClasses.forEach {DataClass dataClass ->
                ItemReference replacementItemReference = replacements.get(dataClass.id)
                if (replacementItemReference != null) {
                    replacement.add((DataClass) replacementItemReference.theItem)
                } else {
                    replacement.add(dataClass)
                }
            }
            dataClasses = replacement
        }
        if (dataElements != null) {
            final List<DataElement> replacement = []
            dataElements.forEach {DataElement dataElement ->
                ItemReference replacementItemReference = replacements.get(dataElement.id)
                if (replacementItemReference != null) {
                    replacement.add((DataElement) replacementItemReference.theItem)
                } else {
                    replacement.add(dataElement)
                }
            }
            dataElements = replacement
        }
        if (referenceTypes != null) {
            final List<DataType> replacement = []
            referenceTypes.forEach {DataType dataType ->
                ItemReference replacementItemReference = replacements.get(dataType.id)
                if (replacementItemReference != null) {
                    replacement.add((DataType) replacementItemReference.theItem)
                } else {
                    replacement.add(dataType)
                }
            }
            referenceTypes = replacement
        }
        if (extendsDataClasses != null) {
            final List<DataClass> replacementSet = []
            extendsDataClasses.forEach {DataClass dataClass ->
                ItemReference replacementItemReference = replacements.get(dataClass.id)
                if (replacementItemReference != null) {
                    dataClass.extendedBy.remove(this)
                    replacementSet.add((DataClass) replacementItemReference.theItem)
                    ((DataClass) replacementItemReference.theItem).extendedBy.add(this)
                } else {
                    replacementSet.add(dataClass)
                }
            }
            extendsDataClasses = replacementSet
        }
    }
}
