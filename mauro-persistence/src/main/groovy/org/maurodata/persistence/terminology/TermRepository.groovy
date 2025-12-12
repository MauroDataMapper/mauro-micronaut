package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.persistence.terminology.dto.TermDTORepository

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRepository implements ModelItemRepository<Term> {

    @Inject
    TermDTORepository termDTORepository

    @Override
    @Nullable
    Term findById(UUID id) {
        log.debug 'TermRepository::findById'
        termDTORepository.findById(id) as Term
    }

    @Nullable
    List<Term> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        termDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }

    @Nullable
    List<Term> findAllByTerminology(Terminology terminology) {
        termDTORepository.findAllByTerminology(terminology) as List<Term>
    }


    @Nullable
    Term findAllByTerminologyAndCode(Terminology terminology, String label) {
        termDTORepository.findAllByTerminologyAndCode(terminology, label) as Term
    }

    @Override
    @Nullable
    List<Term> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    @Nullable
    abstract List<Term> readAllByTerminology(Terminology terminology)

    @Nullable
    abstract List<Term> readAllByTerminologyIdIn(Collection<UUID> terminologyIds)

    @Override
    @Nullable
    List<Term> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Override
    @Nullable
    List<Term> findAllByLabel(String label){
        termDTORepository.findAllByLabel(label)
    }

    abstract Long deleteByTerminologyId(UUID terminologyId)

//    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }


    @Query('''select * from terminology.term
              where term.terminology_id=:terminologyId
              and ((exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.source_term_id=term.id and tr.target_term_id=:id and trt.child_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId)
              or exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.source_term_id=:id and tr.target_term_id=term.id and trt.parental_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId))
              or (:id is null and not exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id 
              and ((tr.target_term_id=term.id and trt.parental_relationship) or (tr.source_term_id=term.id and trt.child_relationship)) and tr.terminology_id=:terminologyId
               and trt.terminology_id=:terminologyId)))''')
    @Nullable
    abstract List<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id)

    @Query(''' select * from terminology.code_set cs
    where exists (select term_id from terminology.code_set_term cst
                    where cst.term_id = :uuid and cst.code_set_id = cs.id) ''')
    @Nullable
    abstract List<CodeSet> getCodeSets(@NonNull UUID uuid)

    Set<Term> findAllByCodeSetsIdIn(@NonNull List<UUID> uuids) {
        termDTORepository.findAllByCodeSetsIdIn(uuids) as Set<Term>
    }

    @Override
    Class getDomainClass() {
        Term
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'tm'.equalsIgnoreCase(pathPrefix)
    }
}
