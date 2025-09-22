package org.maurodata.persistence.dataflow.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassComponentDTORepository implements GenericRepository<DataClassComponentDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'dataFlow', type = Join.Type.LEFT_FETCH)
    @Join(value = 'sourceDataClasses', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetDataClasses', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataClassComponentDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'dataFlow', type = Join.Type.LEFT_FETCH)
    @Join(value = 'sourceDataClasses', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetDataClasses', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataClassComponentDTO> findAllByDataFlowId(UUID uuid)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'dataFlow', type = Join.Type.LEFT_FETCH)
    @Join(value = 'sourceDataClasses', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetDataClasses', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM dataflow.data_class_component WHERE data_flow_id = :item AND label = :pathIdentifier')
    abstract List<DataClassComponentDTO> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)


    @Nullable
    abstract List<DataClassComponentDTO> findAllByLabelContaining(String label)
}
