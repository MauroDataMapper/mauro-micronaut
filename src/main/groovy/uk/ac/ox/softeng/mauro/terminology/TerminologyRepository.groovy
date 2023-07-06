package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.ModelRepository

import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ReactorPageableRepository<Terminology, UUID>, ModelRepository<Terminology> {


    /* todo: use sql:
    SELECT terminology.*, term.*, term_relationship.*, term_relationship_type.*
FROM terminology
LEFT JOIN term ON terminology.id = term.terminology_id
LEFT JOIN term_relationship ON term.id IN (source_term_id, target_term_id)
LEFT JOIN term_relationship_type ON terminology.id = term_relationship_type.terminology_id;
     */
    @Join(value = 'terms', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationshipTypes', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationships', type = Join.Type.LEFT_FETCH)
    abstract Mono<Terminology> findById(UUID id)

    abstract Mono<Terminology> readById(UUID id)

    @Override
    Boolean handles(Class clazz) {
        clazz == Terminology
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['terminology', 'terminologies']
    }
}
