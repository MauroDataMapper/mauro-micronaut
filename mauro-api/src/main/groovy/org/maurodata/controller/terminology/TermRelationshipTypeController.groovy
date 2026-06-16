package org.maurodata.controller.terminology

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.api.Paths
import org.maurodata.api.terminology.TermRelationshipTypeApi
import org.maurodata.audit.Audit
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository

import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class TermRelationshipTypeController extends AdministeredItemController<TermRelationshipType, Terminology> implements TermRelationshipTypeApi {

    TermRelationshipTypeController(TermRelationshipTypeCacheableRepository termRelationshipTypeRepository, TerminologyCacheableRepository terminologyRepository) {
        super(TermRelationshipType, termRelationshipTypeRepository, terminologyRepository)
    }

    @Audit
    @Operation(operationId = 'showTermRelationshipType', summary = "Get a term relationship type", description = "Returns a term relationship type.")
    @Get(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createTermRelationshipType', summary = "Create a term relationship type", description = "Creates a term relationship type.")
    @Post(Paths.TERM_RELATIONSHIP_TYPE_LIST)
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType) {
        termRelationshipType.displayLabel = termRelationshipType.createDisplayLabel()
        super.create(terminologyId, termRelationshipType)
    }

    @Audit
    @Operation(operationId = 'updateTermRelationshipType', summary = "Update a term relationship type", description = "Updates a term relationship type.")
    @Put(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.update(id, termRelationshipType)
    }

    @Audit
    @Operation(operationId = 'listTermRelationshipTypePaged', summary = "List the term relationship types", description = "Returns the term relationship types.")
    @Get(Paths.TERM_RELATIONSHIP_TYPE_LIST_PAGED)
    ListResponse<TermRelationshipType> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(terminologyId, params)
    }

    @Audit(deletedObjectDomainType = TermRelationshipType, parentDomainType = Terminology, parentIdParamName = "terminologyId")
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteTermRelationshipType', summary = "Delete a term relationship type", description = "Deletes a term relationship type.")
    @Delete(Paths.TERM_RELATIONSHIP_TYPE_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType) {
        super.delete(id, termRelationshipType)
    }
}
