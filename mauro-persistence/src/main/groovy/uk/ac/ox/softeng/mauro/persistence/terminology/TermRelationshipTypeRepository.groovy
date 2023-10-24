package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipTypeRepository implements ReactorPageableRepository<TermRelationshipType, UUID>, ModelItemRepository<TermRelationshipType> {

    abstract Mono<TermRelationshipType> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<TermRelationshipType> readByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    abstract Flux<TermRelationshipType> readAllByTerminology(Terminology terminology)

    @Override
    Mono<TermRelationshipType> findByParentIdAndId(UUID parentId, UUID id) {
        findByTerminologyIdAndId(parentId, id)
    }

    @Override
    Mono<TermRelationshipType> readByParentIdAndId(UUID parentId, UUID id) {
        readByTerminologyIdAndId(parentId, id)
    }

    @Override
    Flux<TermRelationshipType> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == TermRelationshipType
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['termrelationshiptype', 'termrelationshiptypes']
    }
}
