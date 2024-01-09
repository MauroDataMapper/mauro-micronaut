package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermDTORepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRepository implements ReactorPageableRepository<Term, UUID>, ModelItemRepository<Term> {

    @Inject
    TermDTORepository termDTORepository

    @Override
    Mono<Term> findById(UUID id) {
        termDTORepository.findById(id) as Mono<Term>
    }

    Flux<Term> findAllByTerminology(Terminology terminology) {
        termDTORepository.findAllByTerminology(terminology) as Flux<Term>
    }

    @Override
    Flux<Term> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract Flux<Term> readAllByTerminology(Terminology terminology)

    @Override
    Flux<Term> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Query('''select * from terminology.term
              where term.terminology_id=:terminologyId
              and ((exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.source_term_id=term.id and tr.target_term_id=:id and trt.child_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId)
              or exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.source_term_id=:id and tr.target_term_id=term.id and trt.parental_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId))
              or (:id is null and not exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id 
              and ((tr.target_term_id=term.id and trt.parental_relationship) or (tr.source_term_id=term.id and trt.child_relationship)) and tr.terminology_id=:terminologyId
               and trt.terminology_id=:terminologyId)))''')
    abstract Flux<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id)

    @Override
    Boolean handles(Class clazz) {
        clazz == Term
    }
}
