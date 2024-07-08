package uk.ac.ox.softeng.mauro.persistence.dataflow.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

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
}
