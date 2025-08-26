package org.maurodata.persistence.datamodel.dto

import org.maurodata.domain.datamodel.EnumerationValue

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.datamodel.DataType

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EnumerationValueDTORepository implements GenericRepository<EnumerationValueDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract EnumerationValueDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract EnumerationValueDTO findAllByEnumerationType(DataType dataType)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract Set<EnumerationValueDTO> findAllByEnumerationTypeIn(Collection<DataType> dataTypes)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM datamodel.enumeration_value WHERE enumeration_type_id = :item AND label = :pathIdentifier')
    abstract List<EnumerationValue> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)
}
