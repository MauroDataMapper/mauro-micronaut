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
@Indexes([@Index(columns = ['terminology_id', 'label'], unique = true)])
class TermRelationshipType {

    @Id
    @GeneratedValue
    UUID id

    @JsonIgnore
    Terminology terminology

    @Version
    Integer version

    String label

    Boolean parentRelationship

    Boolean childRelationship
}
