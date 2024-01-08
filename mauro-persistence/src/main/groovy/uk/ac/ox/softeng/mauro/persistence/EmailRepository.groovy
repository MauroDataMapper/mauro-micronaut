package uk.ac.ox.softeng.mauro.persistence

import uk.ac.ox.softeng.mauro.domain.Email

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class EmailRepository implements ReactorPageableRepository<Email, UUID> {

    abstract Mono<Email> findById(UUID id)

    abstract Mono<Email> findByIdAndVersion(UUID id, Integer version)

    abstract Mono<Email> readById(UUID id)

    abstract Mono<Email> readByIdAndVersion(UUID id, Integer version)

/*
    @Override
    Boolean handles(Class clazz) {
        clazz == Email
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['email', 'emails']
    }
 */

}