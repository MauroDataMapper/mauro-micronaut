package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassDTORepository implements GenericRepository<DataClassDTO, UUID> {

    abstract DataClassDTO findById(UUID id)

    abstract DataClassDTO findAllByDataModel(DataModel dataModel)

    abstract DataClassDTO findAllByParentDataClass(DataClass parentDataClass)

}
