package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class TermRelationshipController extends AdministeredItemController<TermRelationship, Terminology> implements TermRelationshipApi {

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

    @Get(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Post(Paths.TERM_RELATIONSHIP_LIST)
    TermRelationship create(UUID terminologyId, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.EDITOR, terminology)

        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)

        createEntity(terminology, termRelationship)
    }

    @Put(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        TermRelationship existing = termRelationshipRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, existing)
        updateProperties(existing, termRelationship)

        termRelationship.terminology = terminologyRepository.readById(terminologyId)
        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)

        updateEntity(existing, termRelationship)
    }

    @Delete(Paths.TERM_RELATIONSHIP_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(id, termRelationship)
    }

    @Get(Paths.TERM_RELATIONSHIP_LIST)
    ListResponse<TermRelationship> list(UUID terminologyId) {
        super.list(terminologyId)
    }
}
