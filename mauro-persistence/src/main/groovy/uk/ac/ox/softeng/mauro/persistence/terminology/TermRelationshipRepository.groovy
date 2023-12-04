package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermRelationshipDTORepository

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipRepository extends ModelItemRepository<TermRelationship> implements ReactorPageableRepository<TermRelationship, UUID> {

    @Inject
    TermRelationshipDTORepository termRelationshipDTORepository

    @Override
    Mono<TermRelationship> findById(UUID id) {
        termRelationshipDTORepository.findById(id) as Mono<TermRelationship>
    }

    Flux<TermRelationship> findAllByTerminology(Terminology terminology) {
        termRelationshipDTORepository.findAllByTerminology(terminology) as Flux<TermRelationship>
    }

    @Override
    Flux<TermRelationship> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract Flux<TermRelationship> readAllByTerminology(Terminology terminology)

    @Override
    Flux<TermRelationship> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == TermRelationship
    }
}
