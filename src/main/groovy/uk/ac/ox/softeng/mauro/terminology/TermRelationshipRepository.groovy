package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipRepository implements ReactorPageableRepository<TermRelationship, UUID> {

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    abstract Mono<TermRelationship> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<TermRelationship> readByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)
}
