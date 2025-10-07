package org.maurodata.persistence.datamodel.dto

import org.maurodata.domain.datamodel.DataType

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
abstract class DataTypeDTORepository implements GenericRepository<DataTypeDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataTypeDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataTypeDTO> findAllByDataModel(DataModel dataModel)

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    abstract List<DataTypeDTO> findAllByReferenceClassId(UUID referenceClassId)

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    abstract DataTypeDTO findByLabel(String label)

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    abstract List<DataTypeDTO> findByReferenceClassIdIn(List<UUID> referenceClassIds)

    @Nullable
    @Query('SELECT * FROM datamodel.data_type WHERE label = :pathIdentifier AND data_model_id = :item')
    abstract List<DataType> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)



    @Query('SELECT * FROM datamodel.data_type WHERE label = :label')
    @Nullable
    abstract List<DataType> findAllByLabel(String label)
}
