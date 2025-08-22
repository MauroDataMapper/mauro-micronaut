package org.maurodata.persistence.terminology.dto

import org.maurodata.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyDTORepository implements GenericRepository<TerminologyDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract TerminologyDTO findById(UUID id)

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM terminology.terminology WHERE folder_id = :item AND label = :pathIdentifier')
    abstract List<Terminology> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)
}
