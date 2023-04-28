package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.ModelRepository

import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ReactorPageableRepository<Terminology, UUID>, ModelRepository<Terminology> {

    @Join(value = 'terms', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationshipTypes', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationships', type = Join.Type.LEFT_FETCH)
    abstract Mono<Terminology> findById(UUID id)

    abstract Mono<Terminology> readById(UUID id)

    @Override
    Boolean handles(Class clazz) {
        clazz == Terminology
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['terminology', 'terminologies']
    }
}
