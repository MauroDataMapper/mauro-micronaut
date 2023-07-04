package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id', 'label'], unique = true)])
class TermRelationshipType extends ModelItem<Terminology> {

    @Transient
    String domainType = TermRelationshipType.simpleName

    @JsonIgnore
    Terminology terminology

    Boolean parentRelationship

    Boolean childRelationship
}
