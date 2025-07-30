package org.maurodata.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipDTORepository implements GenericRepository<TermRelationshipDTO, UUID> {

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'relationshipType', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract TermRelationshipDTO findById(UUID id)

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'relationshipType', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<TermRelationshipDTO> findAllByTerminology(Terminology terminology)

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'relationshipType', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<TermRelationshipDTO> findAllByTerminologyAndSourceTermOrTargetTerm(Terminology terminology, Term sourceTerm, Term targetTerm)
}
