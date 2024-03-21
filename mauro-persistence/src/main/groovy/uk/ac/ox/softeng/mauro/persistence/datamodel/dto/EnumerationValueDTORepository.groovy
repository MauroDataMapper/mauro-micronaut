package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import uk.ac.ox.softeng.mauro.domain.datamodel.DataType

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EnumerationValueDTORepository implements GenericRepository<EnumerationValueDTO, UUID> {

    abstract EnumerationValueDTO findById(UUID id)

    abstract EnumerationValueDTO findAllByEnumerationType(DataType dataType)
}
