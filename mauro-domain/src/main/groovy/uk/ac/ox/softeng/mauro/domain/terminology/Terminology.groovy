package uk.ac.ox.softeng.mauro.domain.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.Model

import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity
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
}
