package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataTypeDTORepository implements GenericRepository<DataTypeDTO, UUID> {

    abstract DataTypeDTO findById(UUID id)

    abstract List<DataTypeDTO> findAllByDataModel(DataModel dataModel)
}
