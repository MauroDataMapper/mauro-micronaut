package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementDTORepository implements GenericRepository<DataElementDTO, UUID> {

    abstract DataElementDTO findById(UUID id)

    abstract List<DataElementDTO> findAllByDataClass(DataClass dataClass)

    abstract List<DataElementDTO> findAllByDataClassIn(Collection<DataClass> dataClasses)

}
