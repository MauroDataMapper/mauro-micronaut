package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.Term

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRepository implements ReactorPageableRepository<Term, UUID>, ModelItemRepository<Term> {

    static final String FIND_QUERY_SQL = '''select term.*,
        (select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = term.id) as metadata
        FROM terminology.term
        WHERE term.id = :id
        '''

    static final String FIND_ALL_QUERY_SQL = '''select term.*,
        (select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = term.id) as metadata
        FROM terminology.term
        WHERE term.terminology_id = :id
        '''

    Mono<Term> findById(UUID id) {
        findTermDTOById(id) as Mono<Term>
    }

    Flux<Term> findAllByTerminologyId(UUID id) {
        findAllTermDTOByTerminologyId(id) as Flux<Term>
    }

    @Query(FIND_QUERY_SQL)
    abstract Mono<TermDTO> findTermDTOById(UUID id)

    @Query(FIND_ALL_QUERY_SQL)
    abstract Flux<TermDTO> findAllTermDTOByTerminologyId(UUID id)

    abstract Mono<Term> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Term> readByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Boolean> existsByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    abstract Flux<Term> readAllByTerminology(Terminology terminology)

    @Override
    Mono<Term> findByParentIdAndId(UUID parentId, UUID id) {
        findByTerminologyIdAndId(parentId, id)
    }

    @Override
    Mono<Term> readByParentIdAndId(UUID parentId, UUID id) {
        readByTerminologyIdAndId(parentId, id)
    }

    @Override
    Flux<Term> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Query('''select * from terminology.term
              where term.terminology_id=:terminologyId
              and (exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.
              source_term_id=term.id and tr.target_term_id=:id and trt.child_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId)
              or exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id and tr.
              source_term_id=:id and tr.target_term_id=term.id and trt.parental_relationship and tr.terminology_id=:terminologyId and trt.terminology_id=:terminologyId))
              or (:id is null and not exists (select * from terminology.term_relationship tr join terminology.term_relationship_type trt on tr.relationship_type_id=trt.id 
              and ((tr.target_term_id=term.id and trt.parental_relationship) or (tr.source_term_id=term.id and trt.child_relationship)) and tr.terminology_id=:terminologyId
               and trt.terminology_id=:terminologyId))''')
    abstract Flux<Term> childTermsByParent(UUID terminologyId, @Nullable UUID id)


    @Override
    Boolean handles(Class clazz) {
        clazz == Term
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['term', 'terms']
    }
}
