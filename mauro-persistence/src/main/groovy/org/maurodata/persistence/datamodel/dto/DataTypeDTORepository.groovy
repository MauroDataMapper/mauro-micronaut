package org.maurodata.persistence.datamodel.dto


import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
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
}
