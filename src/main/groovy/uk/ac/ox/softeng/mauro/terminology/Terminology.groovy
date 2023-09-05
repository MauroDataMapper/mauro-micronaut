package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.Model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation

@CompileStatic
@Introspected
@MappedEntity
class Terminology extends Model {

//    String modelType = Terminology.simpleName

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<Term> terms

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationshipType> termRelationshipTypes

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationship> termRelationships
}
