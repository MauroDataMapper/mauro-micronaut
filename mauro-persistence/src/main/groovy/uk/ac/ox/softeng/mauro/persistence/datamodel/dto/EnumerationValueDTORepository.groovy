package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EnumerationValueDTORepository implements GenericRepository<EnumerationValueDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract EnumerationValueDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract EnumerationValueDTO findAllByEnumerationType(DataType dataType)
}
