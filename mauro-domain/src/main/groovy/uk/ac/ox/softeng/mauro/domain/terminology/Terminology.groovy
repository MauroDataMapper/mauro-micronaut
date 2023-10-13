package uk.ac.ox.softeng.mauro.domain.terminology


import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.transform.NamedDelegate
import groovy.transform.NamedParam
import groovy.transform.NamedParams
import groovy.transform.NamedVariant
import groovy.transform.ToString
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true)
class Terminology extends Model {
    
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<Term> terms = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationshipType> termRelationshipTypes = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationship> termRelationships = []


    static Terminology build(Map args, @DelegatesTo(value = Terminology.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Terminology(args).tap(closure)
    }

    static Terminology build(@DelegatesTo(value = Terminology.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    Term term(Term term) {
        this.terms.add(term)
        term.terminology = this
        term
    }

    Term term(Map args, @DelegatesTo(value = Term.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Term t = Term.build(args, closure)
        t.terminology = this
        this.terms.add(t)
        t
    }

    Term term(@DelegatesTo(value = Term.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        term [:], closure
    }

    TermRelationshipType termRelationshipType(TermRelationshipType termRelationshipType) {
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType.terminology = this
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(Map args, @DelegatesTo(value = TermRelationshipType.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        TermRelationshipType termRelationshipType = TermRelationshipType.build(args, closure)
        termRelationshipType.terminology = this
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(@DelegatesTo(value = TermRelationshipType.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        termRelationshipType [:], closure
    }

    TermRelationship termRelationship(TermRelationship termRelationship) {
        this.termRelationships.add(termRelationship)
        termRelationship.terminology = this
        termRelationship
    }

    TermRelationship termRelationship(Map args, @DelegatesTo(value = TermRelationship.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        TermRelationship termRelationship = TermRelationship.build(args)
        termRelationship.terminology = this
        this.termRelationships.add(termRelationship)
        termRelationship.tap(closure)
    }

    TermRelationship termRelationship(@DelegatesTo(value = TermRelationship.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        termRelationship [:], closure
    }


}
