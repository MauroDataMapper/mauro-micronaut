package org.maurodata.persistence.dataflow.dto

import org.maurodata.domain.dataflow.DataFlow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.datamodel.DataModel

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataFlowDTORepository implements GenericRepository<DataFlowDTO, UUID> {

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'source', type = Join.Type.LEFT_FETCH)
    @Join(value = 'target', type = Join.Type.LEFT_FETCH)
    abstract DataFlowDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'source', type = Join.Type.LEFT_FETCH)
    @Join(value = 'target', type = Join.Type.LEFT_FETCH)
    abstract List<DataFlowDTO> findAllByTarget(DataModel target)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'source', type = Join.Type.LEFT_FETCH)
    @Join(value = 'target', type = Join.Type.LEFT_FETCH)
    abstract List<DataFlowDTO> findAllBySource(DataModel source)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'source', type = Join.Type.LEFT_FETCH)
    @Join(value = 'target', type = Join.Type.LEFT_FETCH)
    @Query('SELECT * FROM dataflow.data_flow WHERE target_id = :item AND label = :pathIdentifier')
    abstract List<DataFlow> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)



    @Query('SELECT * FROM dataflow.data_flow WHERE label = :label')
    @Nullable
    abstract List<DataFlow> findAllByLabel(String label)
}
