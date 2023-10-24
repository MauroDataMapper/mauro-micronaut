package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import reactor.util.function.Tuple4

@Controller('/terminologies/{terminologyId}/termRelationships')
class TermRelationshipController extends AdministeredItemController<TermRelationship, Terminology> {

    TermRelationshipRepository termRelationshipRepository

    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Inject
    TermRelationshipTypeRepository termRelationshipTypeRepository

    TermRelationshipController(TermRelationshipRepository termRelationshipRepository, TerminologyRepository terminologyRepository,
                               AdministeredItemContentRepository<TermRelationship> administeredItemContentRepository) {
        super(TermRelationship, termRelationshipRepository, terminologyRepository, administeredItemContentRepository)
        this.termRelationshipRepository = termRelationshipRepository
        this.terminologyRepository = terminologyRepository
    }

    @Get('/{id}')
    Mono<TermRelationship> show(UUID terminologyId, UUID id) {
        super.show(terminologyId, id)
    }

    @Post
    Mono<TermRelationship> create(UUID terminologyId, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        Mono.zip(terminologyRepository.readById(terminologyId), termRepository.readByTerminologyIdAndId(terminologyId, termRelationship.sourceTerm.id),
                 termRepository.readByTerminologyIdAndId(terminologyId, termRelationship.targetTerm.id),
                 termRelationshipTypeRepository.readByTerminologyIdAndId(terminologyId, termRelationship.relationshipType.id)).flatMap {Tuple4<Terminology, ?, ?, ?> tuple ->
            Terminology terminology = tuple.getT1()

            createEntity(terminology, termRelationship)
        }
    }

    @Put('/{id}')
    Mono<TermRelationship> update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        termRelationshipRepository.findByTerminologyIdAndId(terminologyId, id).flatMap {TermRelationship existing ->
            updateProperties(existing, termRelationship)

            Mono.zip(terminologyRepository.readById(terminologyId), termRepository.readByTerminologyIdAndId(terminologyId, termRelationship.sourceTerm.id),
                     termRepository.readByTerminologyIdAndId(terminologyId, termRelationship.targetTerm.id),
                     termRelationshipTypeRepository.readByTerminologyIdAndId(terminologyId, termRelationship.relationshipType.id)).flatMap {Tuple4 tuple ->
                updateEntity(existing, termRelationship)
            }
        }
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(terminologyId, id, termRelationship)
    }

    @Get
    Mono<ListResponse<TermRelationship>> list(UUID terminologyId) {
        super.list(terminologyId)
    }
}
