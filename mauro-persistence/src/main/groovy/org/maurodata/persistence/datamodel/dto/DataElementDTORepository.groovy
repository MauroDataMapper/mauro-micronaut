package org.maurodata.persistence.datamodel.dto

import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.datamodel.DataClass

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementDTORepository implements GenericRepository<DataElementDTO, UUID> {

    @Join(value = 'dataType', type = Join.Type.LEFT_FETCH)
    abstract DataElementDTO findById(UUID id)

    abstract List<DataElementDTO> findAllByDataClass(DataClass dataClass)

    abstract List<DataElementDTO> findAllByDataClassIn(Collection<DataClass> dataClasses)

    abstract List<DataElementDTO> readAllByDataTypeIdIn(Collection<UUID> ids)

    @Query('SELECT * FROM datamodel.data_element WHERE data_class_id = :item AND label = :pathIdentifier')
    abstract List<DataElement> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Query('SELECT * FROM datamodel.data_element WHERE label = :label')
    @Nullable
    abstract DataElement findByLabel(String label)
}
