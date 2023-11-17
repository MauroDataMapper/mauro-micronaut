package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Pattern

/**
 * A term describes a value with a code and a meaning, within the context of a terminology.
 * <p>
 * Relationships may be defined between terms, and they may be re-used as part of a codeset - a collection of terms
 * taken from one or more terminologies.
 *
 * @see uk.ac.ox.softeng.mauro.domain.terminology.Terminology
 */
@CompileStatic
@AutoClone(excludes = ['enumerationType'])
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['enumeration_value_id', 'category', 'key', 'value'], unique = true)])
class EnumerationValue extends ModelItem<DataModel> {

    @Override
    String getLabel() {
        key
    }

    @JsonIgnore
    DataType enumerationType

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

    @Transient
    @JsonIgnore
    @Override
    Model getOwner() {
        return this.enumerationType.owner
    }

}
