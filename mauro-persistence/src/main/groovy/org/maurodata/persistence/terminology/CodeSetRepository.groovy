package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.FieldConstants
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.persistence.terminology.dto.CodeSetDTORepository
import org.maurodata.persistence.terminology.dto.CodeSetTermDTO

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetRepository implements ModelRepository<CodeSet> {

    CodeSetRepository(ContentsService contentsService) {
        this.contentsService = contentsService
    }

    @Inject
    CodeSetDTORepository codeSetDTORepository

    @Nullable
    CodeSet findById(UUID id) {
        codeSetDTORepository.findById(id) as CodeSet
    }

    @Nullable
    List<CodeSet> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        codeSetDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }



    @Nullable
    @Override
    List<CodeSet> findAllByLabel(String label){
        codeSetDTORepository.findAllByLabel(label)
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
    @Query(''' delete from terminology.code_set_term cst where cst.code_set_id in (:uuids) ''')
    abstract Long removeTermAssociations(@NonNull Collection<UUID> uuids)

    /**
     * Remove all associations from table code_set_terms
     * @param id codeSetId
     * @returns: number of rows deleted from code_set_terms
     */
    @Query(''' delete from terminology.code_set_term cst where cst.code_set_id = :uuid and cst.term_id = :termId ''')
    abstract Long removeTerm(@NonNull UUID uuid, @NonNull UUID termId)
    /**
     * Add term to codeSet
     * * @param uuid codeSetId
     * * @param termId termId
     * @returns: codeSet
     */
    @Query(''' insert into terminology.code_set_term (code_set_id, term_id) values (:uuid, :termId) ''')
    abstract CodeSet addTerm(@NonNull UUID uuid, @NonNull UUID termId)

    @Query(''' select * from terminology.term t
    where exists (select term_id from terminology.code_set_term cst
                    where cst.code_set_id = :uuid and t.id = cst.term_id) ''')
    @Nullable
    abstract Set<Term> readTerms(@NonNull UUID uuid)

    @Query('''select code_set_id,
           term_id from terminology.code_set_term where code_set_id in (:codeSetIds)''')
    abstract List<CodeSetTermDTO> getCodeSetTerms(@NonNull List<UUID> codeSetIds)


    @Override
    Class getDomainClass() {
        CodeSet
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    @Nullable
    @Override
    abstract List<CodeSet> findAllByFolderId(UUID folderId)

    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in [FieldConstants.CODESET_LOWERCASE, FieldConstants.CODESETS_LOWERCASE]
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'cs'.equalsIgnoreCase(pathPrefix)
    }
}
