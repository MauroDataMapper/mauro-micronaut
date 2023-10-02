package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.Model
import uk.ac.ox.softeng.mauro.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id', 'code'], unique = true)])
class Term extends ModelItem<Terminology> {

    @Transient
    String domainType = 'Term'

    /*String label = code && definition && code == definition ? code :
                   code && definition ? "${code}: ${definition}" :
                   null*/

    String label

    String getLabel() {
        code && definition && code == definition ? code :
        code && definition ? "${code}: ${definition}" :
        null
    }

    @JsonIgnore
    Terminology terminology

    String code

    @Nullable
    String definition

    @Nullable
    String url

    @Nullable
    Boolean isParent

    @Nullable
    Integer depth

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @Nullable
    List<TermRelationship> sourceTermRelationships

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @Nullable
    List<TermRelationship> targetTermRelationships

    @Override
    @Transient
    @JsonIgnore
    Terminology getParent() {
        terminology
    }
}
