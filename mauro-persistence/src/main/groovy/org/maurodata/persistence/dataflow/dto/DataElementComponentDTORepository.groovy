package org.maurodata.persistence.dataflow.dto

import org.maurodata.domain.dataflow.DataClassComponent

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementComponentDTORepository implements GenericRepository<DataElementComponentDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'dataClassComponent', type = Join.Type.LEFT_FETCH)
    @Join(value = 'sourceDataElements', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetDataElements', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataElementComponentDTO> findAllByDataClassComponent(DataClassComponent dataClassComponent)


    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'dataClassComponent', type = Join.Type.LEFT_FETCH)
    @Join(value = 'sourceDataElements', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetDataElements', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataElementComponentDTO findById(UUID id)
}