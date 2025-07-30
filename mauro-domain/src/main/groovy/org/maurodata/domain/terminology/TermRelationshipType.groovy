package org.maurodata.domain.terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.CaseFormat
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
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A TermRelationshipType defines a kind of relationship between terms with a terminology.
 * <p>
 * The TermRelationshipType is identified by its label, and so labels must be distinct within a Terminology.
 * A relationship type may be flagged as conforming to the notion of 'is broader than' (parentalRelationship),
 * or 'is narrower than' (childRelationship), which may assist with reasoning, and helps the interface to present
 * a tree of terms.
 */
@CompileStatic
@AutoClone(excludes = 'terminology')
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['terminology_id', 'label'], unique = true)])
class TermRelationshipType extends ModelItem<Terminology> {

    @JsonIgnore
    Terminology terminology

    @Nullable
    Boolean parentalRelationship

    @Nullable
    Boolean childRelationship

    String displayLabel = displayLabel ?: createDisplayLabel()

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'relationshipType')
    List<TermRelationship> termRelationships = []

    @Transient
    @JsonIgnore
    List<List<TermRelationship>> getAllAssociations() {
        [termRelationships]
    }

    @Override
    @Transient
    @JsonIgnore
    Terminology getParent() {
        terminology
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem terminology) {
        this.terminology = (Terminology) terminology
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'trt'
    }

    String createDisplayLabel() {
        displayLabel = label
        if (!displayLabel) return
        // Replace all spaces and hyphens with underscores
        displayLabel = displayLabel.replaceAll(/[ \-]/, '_')
        // Convert all camel casing to underscores
        displayLabel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, displayLabel)

        // Replace all underscores with spaces and trim to 1 space
        displayLabel = displayLabel.replaceAll(/_/, ' ').replaceAll(/ {2}/, ' ')

        // Capitalise each word in the label
        displayLabel.split().collect {it.capitalize()}.join(' ')
    }

    /****
     * Methods for building a tree-like DSL
     */

    Boolean parentalRelationship(Boolean parentalRelationship) {
        this.parentalRelationship = parentalRelationship
    }

    Boolean childRelationship(Boolean childRelationship) {
        this.childRelationship = childRelationship
    }

    static TermRelationshipType build(
            Map args,
            @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new TermRelationshipType(args).tap(closure)
    }

    static TermRelationshipType build(
            @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }
}
