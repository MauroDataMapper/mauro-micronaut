package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.GenericRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipTypeDTORepository implements GenericRepository<TermRelationshipTypeDTO, UUID> {

    abstract Mono<TermRelationshipTypeDTO> findById(UUID id)

    abstract Flux<TermRelationshipTypeDTO> findAllByTerminology(Terminology terminology)
}
