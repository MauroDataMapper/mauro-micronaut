package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
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
    @Query(''' delete from terminology.code_set_term cst where cst.code_set_id = :uuid ''')
    abstract Long removeTermAssociations(@NonNull UUID uuid)

    /**
     * Remove all associations from table code_set_terms
     * @param id codeSetId
     * @returns: number of rows deleted from code_set_terms
     */
    @Query(''' delete from terminology.code_set_term cst where cst.code_set_id = :uuid and cst.term_id = :termId ''')
    abstract Long removeTerm(@NonNull UUID uuid, @NonNull UUID termId)

    @Query(''' select * from terminology.term t
    where exists (select term_id from terminology.code_set_term cst
                    where cst.code_set_id = :uuid and t.id = cst.term_id) ''')
    @Nullable
    abstract Set<Term> getTerms(@NonNull UUID uuid)

    @Override
    Class getDomainClass() {
        CodeSet
    }

}
