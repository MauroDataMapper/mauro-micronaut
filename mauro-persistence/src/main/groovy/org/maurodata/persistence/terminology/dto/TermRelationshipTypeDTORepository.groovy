package org.maurodata.persistence.terminology.dto

import org.maurodata.domain.terminology.TermRelationshipType

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
abstract class TermRelationshipTypeDTORepository implements GenericRepository<TermRelationshipTypeDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract TermRelationshipTypeDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<TermRelationshipTypeDTO> findAllByTerminology(Terminology terminology)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM terminology.term_relationship_type WHERE terminology_id = :item AND label = :pathIdentifier')
    abstract List<TermRelationshipType> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)


    @Query('SELECT * FROM terminology.term_relationship_type WHERE label = :label')
    @Nullable
    abstract List<TermRelationshipType> findAllByLabel(String label)
}
