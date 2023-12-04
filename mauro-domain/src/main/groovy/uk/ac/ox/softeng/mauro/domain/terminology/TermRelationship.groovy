package uk.ac.ox.softeng.mauro.domain.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 * A TermRelationship defines a relation of a given type between two terms within a terminology.
 * <p>
 * A relationship belongs to a Terminology and defines a relation between two terms within that same terminology.
 * The type of the relationship is also defined within the same context - many relationships can exist of the same type.
 */
@CompileStatic
@AutoClone(excludes = ['terminology'])
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([
        @Index(columns = ['terminology_id']),
        @Index(columns = ['source_term_id']),
        @Index(columns = ['target_term_id']),
        @Index(columns = ['relationship_type_id'])])
class TermRelationship extends ModelItem<Terminology> {

    @JsonIgnore
    Terminology terminology

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'code')
    Term sourceTerm
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'code')
    Term targetTerm

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'label')
    TermRelationshipType relationshipType

    @Override
    String getLabel() {
        relationshipType.label
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
        'tr'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        "$sourceTerm.code.$label.$targetTerm.code"
    }

    /****
     * Methods for building a tree-like DSL
     */

    static TermRelationship build(
            Map args,
            @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new TermRelationship(args).tap(closure)
    }

    static TermRelationship build(
            @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    Term sourceTerm(Term sourceTerm) {
        this.sourceTerm = sourceTerm
    }

    Term sourceTerm(String sourceTermCode) {
        this.sourceTerm = terminology.terms.find { it.code == sourceTermCode }
    }

    Term targetTerm(Term targetTerm) {
        this.targetTerm = targetTerm
    }

    Term targetTerm(String targetTermCode) {
        this.targetTerm = terminology.terms.find { it.code == targetTermCode }
    }

    TermRelationshipType relationshipType(TermRelationshipType relationshipType) {
        this.relationshipType = relationshipType
    }

    TermRelationshipType relationshipType(String relationshipTypeLabel) {
        this.relationshipType = terminology.termRelationshipTypes.
                find { it.label == relationshipTypeLabel }
    }
}
