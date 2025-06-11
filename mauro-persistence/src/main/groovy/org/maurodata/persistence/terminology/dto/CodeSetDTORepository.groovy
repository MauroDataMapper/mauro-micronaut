package org.maurodata.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetDTORepository implements GenericRepository<CodeSetDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract CodeSetDTO findById(UUID id)


}
