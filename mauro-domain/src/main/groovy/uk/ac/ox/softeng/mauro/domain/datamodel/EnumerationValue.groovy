package uk.ac.ox.softeng.mauro.domain.datamodel

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
import uk.ac.ox.softeng.mauro.domain.diff.BaseCollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem

import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 * A term describes a value with a code and a meaning, within the context of a terminology.
 * <p>
 * Relationships may be defined between terms, and they may be re-used as part of a codeset - a collection of terms
 * taken from one or more terminologies.
 *
 * @see uk.ac.ox.softeng.mauro.domain.terminology.Terminology
 */
@CompileStatic
@AutoClone(excludes = ['enumerationType', 'dataModel'])
@Introspected
@MappedEntity(schema = 'datamodel', value = 'enumeration_value')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['enumeration_value_id', 'enumeration_type_id', 'category', 'key', 'value'], unique = true)])
class EnumerationValue extends ModelItem<DataModel> implements DiffableItem<EnumerationValue> {

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
        new BaseCollectionDiff(id,label)
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        this.key
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<EnumerationValue> diff(EnumerationValue other) {
        ObjectDiff<EnumerationValue> base = DiffBuilder.objectDiff(EnumerationValue)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString)
        base.appendString(DiffBuilder.CATEGORY, this.category, other.category)
    }

    /****
     * Methods for building a tree-like DSL
     */

    static EnumerationValue build(
            Map args,
            @DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new EnumerationValue(args).tap(closure)
    }

    static EnumerationValue build(@DelegatesTo(value = EnumerationValue, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
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
}
