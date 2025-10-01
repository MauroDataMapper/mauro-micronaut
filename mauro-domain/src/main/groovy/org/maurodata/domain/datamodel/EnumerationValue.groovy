package org.maurodata.domain.datamodel

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem

import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A term describes a value with a code and a meaning, within the context of a terminology.
 * <p>
 * Relationships may be defined between terms, and they may be re-used as part of a codeset - a collection of terms
 * taken from one or more terminologies.
 *
 * @see org.maurodata.domain.terminology.Terminology
 */
@CompileStatic
@AutoClone(excludes = ['enumerationType', 'dataModel'])
@Introspected
@MappedEntity(schema = 'datamodel', value = 'enumeration_value')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['enumeration_value_id', 'enumeration_type_id', 'category', 'key', 'value'], unique = true)])
class EnumerationValue extends ModelItem<DataModel> implements DiffableItem<EnumerationValue>, ItemReferencer {

    @Override
    String getLabel() {
        key
    }

    @JsonIgnore
    @NotNull
    DataType enumerationType

    @JsonIgnore
    @Transient
    DataModel dataModel

    @Nullable
    String category

    @NotNull
    String key

    @NotNull
    String value

    @Override
    @Transient
    @JsonIgnore
    DataType getParent() {
        enumerationType
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem dataType) {
        this.enumerationType = (DataType) dataType
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'ev'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        key
    }

    @Override
    @Transient
    @JsonIgnore
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, getDiffIdentifier(), label)
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        if (enumerationType != null) {
            return "${enumerationType.getDiffIdentifier()}|${getPathNodeString()}"
        }
        if (dataModel != null) {
            return "${dataModel.getDiffIdentifier()}|${getPathNodeString()}"
        }
        return "${getPathNodeString()}"
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<EnumerationValue> diff(EnumerationValue other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<EnumerationValue> base = DiffBuilder.objectDiff(EnumerationValue)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        base.appendString(DiffBuilder.CATEGORY, this.category, other.category, this, other)
    }

    /****
     * Methods for building a tree-like DSL
     */

    static EnumerationValue build(
        Map args,
        @DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new EnumerationValue(args).tap(closure)
    }

    static EnumerationValue build(@DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    String category(String category) {
        this.category = category
    }

    String key(String key) {
        this.key = key
    }

    String value(String value) {
        this.value = value
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(enumerationType, pathsBeingReferenced)
        ItemReferencerUtils.addItem(dataModel, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        dataModel = ItemReferencerUtils.replaceItemByIdentity(dataModel, replacements, notReplaced)
        enumerationType = ItemReferencerUtils.replaceItemByIdentity(enumerationType, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        EnumerationValue intoEnumerationValue = (EnumerationValue) into
        intoEnumerationValue.enumerationType = ItemUtils.copyItem(this.enumerationType, intoEnumerationValue.enumerationType)
        intoEnumerationValue.dataModel = ItemUtils.copyItem(this.dataModel, intoEnumerationValue.dataModel)
        intoEnumerationValue.category = ItemUtils.copyItem(this.category, intoEnumerationValue.category)
        intoEnumerationValue.key = ItemUtils.copyItem(this.key, intoEnumerationValue.key)
        intoEnumerationValue.value = ItemUtils.copyItem(this.value, intoEnumerationValue.value)
        // depends on key
        intoEnumerationValue.label = ItemUtils.copyItem(this.label, intoEnumerationValue.label)
    }

    @Override
    Item shallowCopy() {
        EnumerationValue enumerationValueShallowCopy = new EnumerationValue()
        this.copyInto(enumerationValueShallowCopy)
        return enumerationValueShallowCopy
    }
}
