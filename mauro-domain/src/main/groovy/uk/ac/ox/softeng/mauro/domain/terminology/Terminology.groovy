package uk.ac.ox.softeng.mauro.domain.terminology


import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
@Introspected
@MappedEntity
class Terminology extends Model {
    
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<Term> terms

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationshipType> termRelationshipTypes

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationship> termRelationships
}
