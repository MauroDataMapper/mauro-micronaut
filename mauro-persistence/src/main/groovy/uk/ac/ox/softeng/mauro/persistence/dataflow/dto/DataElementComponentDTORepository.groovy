package uk.ac.ox.softeng.mauro.persistence.dataflow.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementComponentDTORepository implements GenericRepository<DataElementComponentDTO, UUID> {


    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataElementComponentDTO> findAllByDataClassComponent(DataClassComponent dataClassComponent)


    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataElementComponentDTO findById(UUID id)
}