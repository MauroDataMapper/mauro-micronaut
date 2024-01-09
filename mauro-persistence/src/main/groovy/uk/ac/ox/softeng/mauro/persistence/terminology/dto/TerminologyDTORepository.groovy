package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.GenericRepository
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyDTORepository implements GenericRepository<TerminologyDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract Mono<TerminologyDTO> findById(UUID id)
}
