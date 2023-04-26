package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRepository implements ReactorPageableRepository<Term, UUID> {

    abstract Mono<Term> findByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Boolean> existsByTerminologyIdAndId(UUID terminologyId, UUID id)

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    @Query('''SELECT * FROM term
              WHERE term.terminology_id=:terminologyId
              AND (EXISTS (SELECT * FROM term_relationship tr JOIN term_relationship_type trt ON tr.relationship_type_id=trt.id AND tr.source_term_id=term.id AND tr.target_term_id=:id AND trt.child_relationship AND tr.terminology_id=:terminologyId AND trt.terminology_id=:terminologyId)
              OR EXISTS (SELECT * FROM term_relationship tr JOIN term_relationship_type trt ON tr.relationship_type_id=trt.id AND tr.source_term_id=:id AND tr.target_term_id=term.id AND trt.parent_relationship AND tr.terminology_id=:terminologyId AND trt.terminology_id=:terminologyId))
              OR (:id IS NULL AND NOT EXISTS (SELECT * FROM term_relationship tr JOIN term_relationship_type trt ON tr.relationship_type_id=trt.id AND ((tr.target_term_id=term.id AND trt.parent_relationship) OR (tr.source_term_id=term.id AND trt.child_relationship)) AND tr.terminology_id=:terminologyId AND trt.terminology_id=:terminologyId))''')
    abstract Flux<Term> childTermsByParent(UUID terminologyId, UUID id)

}
