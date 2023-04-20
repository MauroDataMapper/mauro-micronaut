package uk.ac.ox.softeng.mauro.terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version

@CompileStatic
@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id']), @Index(columns = ['source_term_id']), @Index(columns = ['target_term_id']), @Index(columns = ['relationship_type_id'])])
class TermRelationship {

    @Id
    @GeneratedValue
    UUID id

    @JsonIgnore
    Terminology terminology

    @Version
    Integer version

    Term sourceTerm

    Term targetTerm

    TermRelationshipType relationshipType
}
