package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipDTORepository implements GenericRepository<TermRelationshipDTO, UUID> {

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    abstract TermRelationshipDTO findById(UUID id)

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    abstract List<TermRelationshipDTO> findAllByTerminology(Terminology terminology)
}
