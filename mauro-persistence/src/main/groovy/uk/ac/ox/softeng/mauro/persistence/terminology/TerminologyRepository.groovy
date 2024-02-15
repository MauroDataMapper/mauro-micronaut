package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TerminologyDTORepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ModelRepository<Terminology> {

    @Inject
    TerminologyDTORepository terminologyDTORepository

    Terminology findById(UUID id) {
        terminologyDTORepository.findById(id) as Terminology
    }
    @Override
    Class getDomainClass() {
        Terminology
    }
}
