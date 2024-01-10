package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableTerminologyRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.cache.annotation.Cacheable
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

@CompileStatic
@Controller('/terminologies/{terminologyId}/termRelationships')
class TermRelationshipController extends AdministeredItemController<TermRelationship, Terminology> {

    CacheableTermRelationshipRepository termRelationshipRepository

    CacheableTerminologyRepository terminologyRepository

    @Inject
    CacheableTermRepository termRepository

    @Inject
    CacheableTermRelationshipTypeRepository termRelationshipTypeRepository

    TermRelationshipController(CacheableTermRelationshipRepository termRelationshipRepository, CacheableTerminologyRepository terminologyRepository,
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

        Mono.zip(terminologyRepository.readById(terminologyId), termRepository.readById(termRelationship.sourceTerm.id),
                 termRepository.readById(termRelationship.targetTerm.id),
                 termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)).flatMap {Tuple4<Terminology, Term, Term, TermRelationshipType> tuple ->
            Terminology terminology = tuple.getT1()
            termRelationship.sourceTerm = tuple.getT2()
            termRelationship.targetTerm = tuple.getT3()
            termRelationship.relationshipType = tuple.getT4()

            createEntity(terminology, termRelationship)
        }
    }

    @Put('/{id}')
    Mono<TermRelationship> update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        termRelationshipRepository.readById(id).flatMap {TermRelationship existing ->
            updateProperties(existing, termRelationship)

            Mono.zip(terminologyRepository.readById(terminologyId), termRepository.readById(termRelationship.sourceTerm.id),
                     termRepository.readById(termRelationship.targetTerm.id),
                     termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)).flatMap {Tuple4<Terminology, Term, Term, TermRelationshipType> tuple ->
                termRelationship.terminology = tuple.getT1()
                termRelationship.sourceTerm = tuple.getT2()
                termRelationship.targetTerm = tuple.getT3()
                termRelationship.relationshipType = tuple.getT4()

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
