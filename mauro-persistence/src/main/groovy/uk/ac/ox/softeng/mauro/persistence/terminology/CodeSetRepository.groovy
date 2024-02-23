package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.CodeSetDTORepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetRepository implements ModelRepository<CodeSet> {

    @Inject
    CodeSetDTORepository codeSetDTORepository

    @Nullable
    CodeSet findById(UUID id) {
        codeSetDTORepository.findById(id) as CodeSet
    }


    @Override
    Class getDomainClass() {
        CodeSet
    }

}
