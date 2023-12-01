package uk.ac.ox.softeng.mauro.domain.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerator
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
//@JsonIdentityInfo(property = 'label', generator = ObjectIdGenerators.PropertyGenerator)
class TermRelationshipType extends ModelItem<Terminology> {

    @JsonIgnore
    Terminology terminology

    @Nullable
    Boolean parentalRelationship

    @Nullable
    Boolean childRelationship

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
