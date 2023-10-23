package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipRepository implements ReactorPageableRepository<TermRelationship, UUID>, ModelItemRepository<TermRelationship> {

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    abstract Mono<TermRelationship> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<TermRelationship> readByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    abstract Flux<TermRelationship> readAllByTerminology(Terminology terminology)

    @Override
    Mono<TermRelationship> findByParentIdAndId(UUID parentId, UUID id) {
        findByTerminologyIdAndId(parentId, id)
    }

    @Override
    Mono<TermRelationship> readByParentIdAndId(UUID parentId, UUID id) {
        readByTerminologyIdAndId(parentId, id)
    }

    @Override
    Flux<TermRelationship> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == TermRelationship
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['termrelationship', 'termrelationships']
    }
}
