package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermRelationshipTypeDTORepository

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipTypeRepository implements ReactorPageableRepository<TermRelationshipType, UUID>, ModelItemRepository<TermRelationshipType> {

    @Inject
    TermRelationshipTypeDTORepository termRelationshipTypeDTORepository

    Mono<TermRelationshipType> findById(UUID id) {
        termRelationshipTypeDTORepository.findById(id) as Mono<TermRelationshipType>
    }

    Flux<TermRelationshipType> findAllByTerminology(Terminology terminology) {
        termRelationshipTypeDTORepository.findAllByTerminology(terminology) as Flux<TermRelationshipType>
    }

    @Override
    Flux<TermRelationshipType> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract Flux<TermRelationshipType> readAllByTerminology(Terminology terminology)

    @Override
    Flux<TermRelationshipType> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    abstract Mono<Long> deleteByTerminologyId(UUID terminologyId)

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == TermRelationshipType
    }
}
