package org.maurodata.persistence.datamodel.dto

import org.maurodata.domain.datamodel.DataModel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataModelDTORepository implements GenericRepository<DataModelDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataModelDTO findById(UUID id)

//    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM datamodel.data_model WHERE folder_id = :item AND label = :pathIdentifier')
    abstract List<DataModel> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)



    @Query('SELECT * FROM datamodel.data_model WHERE label like :label')
    @Nullable
    abstract List<DataModel> findAllByLabelContaining(String label)
}
