package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.GenericRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassDTORepository implements GenericRepository<DataClassDTO, UUID> {

    abstract Mono<DataClassDTO> findById(UUID id)

    abstract Flux<DataClassDTO> findAllByDataModel(DataModel dataModel)
}
