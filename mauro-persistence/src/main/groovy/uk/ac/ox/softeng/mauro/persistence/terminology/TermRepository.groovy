package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermDTORepository

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRepository implements ModelItemRepository<Term> {

    @Inject
    TermDTORepository termDTORepository

    @Override
    Term findById(UUID id) {
        log.debug 'TermRepository::findById'
        termDTORepository.findById(id) as Term
    }

    List<Term> findAllByTerminology(Terminology terminology) {
        termDTORepository.findAllByTerminology(terminology) as List<Term>
    }

    @Override
    List<Term> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract List<Term> readAllByTerminology(Terminology terminology)

    @Override
    List<Term> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
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
    abstract List<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id)

    @Override
    Class getDomainClass() {
        Term
    }
}
