package uk.ac.ox.softeng.mauro.persistence.terminology

import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipTypeRepository implements ReactorPageableRepository<TermRelationshipType, UUID> {

    abstract Mono<TermRelationshipType> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)
}
