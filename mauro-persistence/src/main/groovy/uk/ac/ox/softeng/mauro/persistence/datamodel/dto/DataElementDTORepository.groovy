package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementDTORepository implements GenericRepository<DataElementDTO, UUID> {

    abstract DataElementDTO findById(UUID id)

    abstract DataElementDTO findAllByDataClass(DataClass dataClass)
}
