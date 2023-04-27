package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Version
import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id', 'code'], unique = true)])
class Term extends ModelItem<Terminology> {

    @Transient
    String domainType = 'Term'

    @Id
    @GeneratedValue
    UUID id

    @JsonIgnore
    Terminology terminology

    @Version
    Integer version

    String code

    @Nullable
    String definition

    @Nullable
    String description

    @Nullable
    String url

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @Nullable
    List<TermRelationship> sourceTermRelationships

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @Nullable
    List<TermRelationship> targetTermRelationships
}
