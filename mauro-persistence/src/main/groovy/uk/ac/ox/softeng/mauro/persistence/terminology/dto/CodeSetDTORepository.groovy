package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Term

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetDTORepository implements GenericRepository<CodeSetDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract CodeSetDTO findById(UUID id)

    @Query(''' delete from terminology.code_set_term cst where cst.code_set_id = :uuid ''')
    abstract Long removeTermAssociations(@NonNull UUID uuid)

    @Query(''' select * from terminology.term t
    where exists (select term_id from terminology.code_set_term cst
                    where cst.code_set_id = :uuid and t.id = cst.term_id) ''')
    @Nullable
    abstract Set<Term> getTerms(@NonNull UUID uuid)
}
