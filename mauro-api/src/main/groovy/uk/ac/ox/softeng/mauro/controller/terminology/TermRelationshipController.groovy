package uk.ac.ox.softeng.mauro.controller.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/terminologies/{terminologyId}/termRelationships')
class TermRelationshipController extends AdministeredItemController<TermRelationship, Terminology> {

    TermRelationshipCacheableRepository termRelationshipRepository

    TerminologyCacheableRepository terminologyRepository

    @Inject
    TermCacheableRepository termRepository

    @Inject
    TermRelationshipTypeCacheableRepository termRelationshipTypeRepository

    TermRelationshipController(TermRelationshipCacheableRepository termRelationshipRepository, TerminologyCacheableRepository terminologyRepository,
                               AdministeredItemContentRepository administeredItemContentRepository) {
        super(TermRelationship, termRelationshipRepository, terminologyRepository, administeredItemContentRepository)
        this.termRelationshipRepository = termRelationshipRepository
        this.terminologyRepository = terminologyRepository
    }

    @Get('/{id}')
    TermRelationship show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Post
    TermRelationship create(UUID terminologyId, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        Terminology terminology = terminologyRepository.readById(terminologyId)
        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)

        createEntity(terminology, termRelationship)
    }

    @Put('/{id}')
    TermRelationship update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        TermRelationship existing = termRelationshipRepository.readById(id)
        updateProperties(existing, termRelationship)

        termRelationship.terminology = terminologyRepository.readById(terminologyId)
        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)

        updateEntity(existing, termRelationship)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(id, termRelationship)
    }

    @Get
    ListResponse<TermRelationship> list(UUID terminologyId) {
        super.list(terminologyId)
    }
}
