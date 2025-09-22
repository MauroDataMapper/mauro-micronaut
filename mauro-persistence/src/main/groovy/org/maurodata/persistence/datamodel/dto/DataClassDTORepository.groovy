package org.maurodata.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassDTORepository implements GenericRepository<DataClassDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataClassDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataClassDTO> findAllByDataModel(DataModel dataModel)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataClassDTO> findAllByParentDataClass(DataClass parentDataClass)

    @Nullable
    @Query('SELECT * FROM datamodel.data_class WHERE label = :pathIdentifier AND ((parent_data_class_id IS NOT NULL AND parent_data_class_id = :item) OR (parent_data_class_id IS NULL AND data_model_id = :item))')
    abstract List<DataClass> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Nullable
    @Query('SELECT * FROM datamodel.data_class WHERE label like :label')
    abstract List<DataClass> findAllByLabelContaining(String label)
}
