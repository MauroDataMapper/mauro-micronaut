package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.*
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 * A Terminology is a model that describes a number of terms, and some relationships between them.
 */
@Slf4j
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['folder_id', 'label', 'branch_name', 'model_version'], unique = true)])
@JsonPropertyOrder(['terms', 'termRelationshipTypes'])
class Terminology extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @JsonIdentityInfo(generator = TermJsonIdGenerator)
    @JsonManagedReference
    List<Term> terms = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'label')
    List<TermRelationshipType> termRelationshipTypes = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationship> termRelationships = []

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'te'
    }

    @Override
    @Transient
    @JsonIgnore
    List<List<ModelItem<Terminology>>> getAllAssociations() {
        [terms, termRelationshipTypes, termRelationships] as List<List<ModelItem<Terminology>>>
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        Map<UUID, Term> termsMap = terms.collectEntries {[it.id, it]}
        Map<UUID, TermRelationshipType> termRelationshipTypesMap = termRelationshipTypes.collectEntries {[it.id, it]}

        terms.each {
            it.parent = this
        }
        termRelationshipTypes.each {
            it.parent = this
        }
        termRelationships.each {
            it.parent = this
            it.relationshipType = termRelationshipTypesMap[it.relationshipType.id]
            it.sourceTerm = termsMap[it.sourceTerm.id]
            it.targetTerm = termsMap[it.targetTerm.id]
        }

        this
    }

    @Override
    Terminology clone() {
        log.debug '*** Terminology.clone() ***'

        Terminology cloned = (Terminology) super.clone()
        cloned.setAssociations()

        cloned
    }

    /****
     * Methods for building a tree-like DSL
     */

    static Terminology build(
            Map args,
            @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Terminology(args).tap(closure)
    }

    static Terminology build(
            @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    Term term(Term term) {
        this.terms.add(term)
        term.terminology = this
        term
    }

    Term term(Map args, @DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        Term t = Term.build(args, closure)
        t.terminology = this
        this.terms.add(t)
        t
    }

    Term term(@DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        term [:], closure
    }

    TermRelationshipType termRelationshipType(TermRelationshipType termRelationshipType) {
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType.terminology = this
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(
            Map args,
            @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        TermRelationshipType termRelationshipType = TermRelationshipType.build(args, closure)
        termRelationshipType.terminology = this
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(
            @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        termRelationshipType [:], closure
    }

    TermRelationship termRelationship(TermRelationship termRelationship) {
        this.termRelationships.add(termRelationship)
        termRelationship.terminology = this
        termRelationship
    }

    TermRelationship termRelationship(
            Map args,
            @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        TermRelationship termRelationship = TermRelationship.build(args)
        termRelationship.terminology = this
        this.termRelationships.add(termRelationship)
        termRelationship.tap(closure)
    }

    TermRelationship termRelationship(
            @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        termRelationship [:], closure
    }
}
