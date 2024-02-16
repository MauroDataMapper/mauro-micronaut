package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermDTORepository implements GenericRepository<TermDTO, UUID> {

    @Nullable
    abstract TermDTO findById(UUID id)

    @Nullable
    abstract List<TermDTO> findAllByTerminology(Terminology terminology)
}
