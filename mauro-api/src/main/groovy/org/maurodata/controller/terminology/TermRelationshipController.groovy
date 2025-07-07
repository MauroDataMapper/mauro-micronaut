package org.maurodata.controller.terminology

import org.maurodata.api.Paths
import org.maurodata.api.terminology.TermRelationshipApi
import org.maurodata.audit.Audit
import org.maurodata.domain.terminology.Term
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository
import org.maurodata.web.ListResponse

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

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
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

    @Audit
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

    @Audit(deletedObjectDomainType = TermRelationship, parentDomainType = Terminology, parentIdParamName = "terminologyId")
    @Delete(Paths.TERM_RELATIONSHIP_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(id, termRelationship)
    }

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_LIST_PAGED)
    ListResponse<TermRelationship> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(terminologyId, params)
    }

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_BY_TERM_ID_LIST)
    ListResponse<TermRelationship> byTerminologyAndTermIdList(UUID terminologyId, UUID termId) {
        Term term = termRepository.readById(termId)
        accessControlService.canDoRole(Role.READER, term)
        List<TermRelationship> termRelationshipsByTerm = (super.listItems(terminologyId) as List<TermRelationship>).findAll {
            it.sourceTerm.id == termId || it.targetTerm.id == termId
        }
        println(termRelationshipsByTerm.size())
        ListResponse.from(termRelationshipsByTerm)
    }
}
