package org.maurodata.controller.terminology

import io.swagger.v3.oas.annotations.Operation
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import org.maurodata.api.Paths
import org.maurodata.api.terminology.TermRelationshipApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository

import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

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

    TermRelationshipController(TermRelationshipCacheableRepository termRelationshipRepository, TerminologyCacheableRepository terminologyRepository) {
        super(TermRelationship, termRelationshipRepository, terminologyRepository)
        this.termRelationshipRepository = termRelationshipRepository
        this.terminologyRepository = terminologyRepository
    }

    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['availableActions']
    }

    @Audit
    @Operation(operationId = 'showTermRelationship', summary = "Get a term relationship", description = "Returns a term relationship.")
    @Get(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createTermRelationship', summary = "Create a term relationship", description = "Creates a term relationship. You must have edit privileges on the item in question.")
    @Post(Paths.TERM_RELATIONSHIP_LIST)
    TermRelationship create(UUID terminologyId, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship)

        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.EDITOR, terminology)
        termRelationship = retrieveRelationships(termRelationship)

        createEntity(terminology, termRelationship)
    }

    @Audit
    @Operation(operationId = 'updateTermRelationship', summary = "Update a term relationship", description = "Updates a term relationship.")
    @Put(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship) {
        updateTermRelationship(termRelationship, id, terminologyId)
    }


    @Audit(deletedObjectDomainType = TermRelationship, parentDomainType = Terminology, parentIdParamName = "terminologyId")
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteTermRelationship', summary = "Delete a term relationship", description = "Deletes a term relationship.")
    @Delete(Paths.TERM_RELATIONSHIP_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(id, termRelationship)
    }

    @Audit
    @Override
    @Operation(operationId = 'listTermRelationshipPaged', summary = "List the term relationships", description = "Returns the term relationships.")
    @Get(Paths.TERM_RELATIONSHIP_LIST_PAGED)
    ListResponse<TermRelationship> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        super.list(terminologyId, params)
    }

    @Audit
    @Operation(operationId = 'listTermRelationshipByTerm', summary = "List the term relationships", description = "Returns the term relationships. You must have read privileges on the item in question.")
    @Get(Paths.TERM_RELATIONSHIP_BY_TERM_ID_LIST)
    @Override
    ListResponse<TermRelationship> byTerminologyAndTermIdList(UUID terminologyId, UUID termId) {
        Term term = termRepository.readById(termId)
        accessControlService.canDoRole(Role.READER, term)
        Terminology terminology = parentItemRepository.readById(terminologyId)
        List<TermRelationship> termRelationshipsByTerm = termRelationshipRepository.findAllByTerminologyAndSourceTermOrTargetTerm(terminology, term)
        ListResponse.from(termRelationshipsByTerm)
    }

    @Audit
    @Override
    @Operation(summary = "Get a term relationship", description = "Returns a term relationship. You must have read privileges on the item in question.")
    @Get(Paths.TERM_RELATIONSHIP_BY_TERM_ID_ID)
    TermRelationship showByTerminologyAndTerm(@NonNull UUID terminologyId, @NonNull UUID termId, @NonNull UUID id) {
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.canDoRole(Role.READER, terminology)
        Term term = termRepository.readById(termId)
        accessControlService.canDoRole(Role.READER, term)
        super.show(id)
    }


    @Audit
    @Transactional
    @Override
    @Operation(operationId = 'createTermRelationshipByTerminologyAndTerm', summary = "Create a term relationship", description = "Creates a term relationship. You must have edit privileges on the item in question.")
    @Post(Paths.TERM_RELATIONSHIP_BY_TERM_ID_LIST)
    TermRelationship createByTerminologyAndTerm(@NonNull UUID terminologyId, @NonNull UUID termId, @Body @NonNull TermRelationship termRelationship) {
        cleanBody(termRelationship, false)
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.EDITOR, terminology)
        Term term = termRepository.readById(termId)
        accessControlService.checkRole(Role.EDITOR, term)

        termRelationship = retrieveRelationships(termRelationship)

        createEntity(terminology, termRelationship)
    }


    @Audit(deletedObjectDomainType = TermRelationship, parentDomainType = Terminology, parentIdParamName = "terminologyId")
    @Transactional
    @Override
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteTermRelationshipById', summary = "Delete a term relationship", description = "Deletes a term relationship.")
    @Delete(Paths.TERM_RELATIONSHIP_BY_TERM_ID_ID)
    HttpResponse delete(UUID terminologyId, @NotNull UUID termId, @NotNull UUID id, @Body @Nullable TermRelationship termRelationship) {
        super.delete(id, termRelationship)
    }

    @Audit
    @Operation(summary = "Update a term relationship", description = "Updates a term relationship. You must have edit privileges on the item in question.")
    @Put(Paths.TERM_RELATIONSHIP_BY_TERM_ID_ID)
    TermRelationship updateByTerminologyAndTerm(@NotNull UUID terminologyId, @NotNull UUID termId, @NotNull UUID id, @Body @NotNull TermRelationship termRelationship) {
        Term term = termRepository.readById(termId)
        accessControlService.checkRole(Role.EDITOR, term)
        updateTermRelationship(termRelationship, id, terminologyId)
    }

    protected TermRelationship updateTermRelationship(TermRelationship termRelationship, UUID id, UUID terminologyId) {
        cleanBody(termRelationship, false)

        TermRelationship existing = termRelationshipRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, existing)
        updateProperties(existing, termRelationship)

        termRelationship.terminology = terminologyRepository.readById(terminologyId)
        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)

        updateEntity(existing, termRelationship)

    }

    protected TermRelationship retrieveRelationships(TermRelationship termRelationship) {
        termRelationship.sourceTerm = termRepository.readById(termRelationship.sourceTerm.id)
        termRelationship.targetTerm = termRepository.readById(termRelationship.targetTerm.id)
        termRelationship.relationshipType = termRelationshipTypeRepository.readById(termRelationship.relationshipType.id)
        termRelationship
    }

}
