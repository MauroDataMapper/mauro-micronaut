package uk.ac.ox.softeng.mauro.domain.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.Model

import jakarta.persistence.Transient

/**
 * A Terminology is a model that describes a number of terms, and some relationships between them.
 */
@Slf4j
@CompileStatic
@AutoClone
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Terminology extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<Term> terms = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
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
    Collection<AdministeredItem> getAllContents() {
        List<AdministeredItem> items = []
        terms?.each {it.terminology = this}
        termRelationshipTypes?.each {it.terminology = this}
        termRelationships?.each {it.terminology = this}
        if (terms) items.addAll(terms)
        if (termRelationshipTypes) items.addAll(termRelationshipTypes)
        if (termRelationships) items.addAll(termRelationships.findAll {terms?.id?.contains(it.id)})
        items
    }

    @Override
    Terminology clone() {
        log.debug '*** Terminology.clone() ***'

        Terminology cloned = (Terminology) super.clone()
        cloned.terms = terms.collect {it.clone().tap {it.parent = cloned}}
        cloned.termRelationshipTypes = termRelationshipTypes.collect {it.clone().tap {it.parent = cloned}}
        cloned.termRelationships = termRelationships.collect {
            it.clone().tap {TermRelationship tr ->
                tr.relationshipType = cloned.termRelationshipTypes.find {it.label == tr.relationshipType.label}
                tr.sourceTerm = cloned.terms.find {it.code == tr.sourceTerm.code}
                tr.targetTerm = cloned.terms.find {it.code == tr.targetTerm.code}
                tr.parent = cloned
            }
        }

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
