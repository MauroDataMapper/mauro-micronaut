package org.maurodata.persistence.terminology.dto

import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.terminology.CodeSet
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetDTORepository implements GenericRepository<CodeSetDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract CodeSetDTO findById(UUID id)

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Query('SELECT * FROM terminology.code_set WHERE folder_id = :item AND label = :pathIdentifier')
    abstract List<CodeSet> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)


    @Nullable
    @Query('SELECT * FROM terminology.code_set WHERE label = :label')
    abstract List<CodeSet> findAllByLabel(String label)
}
