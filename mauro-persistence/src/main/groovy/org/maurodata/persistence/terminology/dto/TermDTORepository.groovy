package org.maurodata.persistence.terminology.dto

import org.maurodata.domain.terminology.Term

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.terminology.Terminology

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermDTORepository implements GenericRepository<TermDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract TermDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<TermDTO> findAllByTerminology(Terminology terminology)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM terminology.term WHERE terminology_id = :item AND label = :pathIdentifier')
    abstract List<Term> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Query('SELECT * FROM terminology.term WHERE label like :label')
    @Nullable
    abstract Term findByLabelContaining(String label)

    @Query('SELECT * FROM terminology.term WHERE label like :label')
    @Nullable
    abstract List<Term> findAllByLabelContaining(String label)
}
