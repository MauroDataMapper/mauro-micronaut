package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
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

    /**
     * Remove all associations from table code_set_terms
     * @param id codeSetId
     * @returns: number of rows deleted from code_set_terms
     */
    Long removeTermAssociations(UUID id){
        codeSetDTORepository.removeTermAssociations(id)
    }
    /**
     * Remove all associations from table code_set_terms
     * @param id codeSetId
     */
    Set<Term> getTerms(UUID id){
        codeSetDTORepository.getTerms(id)
    }

    @Override
    Class getDomainClass() {
        CodeSet
    }

}
