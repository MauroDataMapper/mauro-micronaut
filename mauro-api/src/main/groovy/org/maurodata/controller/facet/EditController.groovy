package org.maurodata.controller.facet

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.api.Paths
import org.maurodata.api.facet.EditApi
import org.maurodata.audit.Audit
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.web.ListResponse

import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class EditController extends FacetController<Edit> implements EditApi {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.EditCacheableRepository editRepository

    EditController(FacetCacheableRepository.EditCacheableRepository editRepository) {
        super(editRepository)
        this.editRepository = editRepository
    }

    @Audit
    @Override
    @Operation(operationId = 'listEditPaged', summary = "List the edits", description = "Returns the edits. You must have read privileges on the item in question.")
    @Get(Paths.EDIT_LIST_PAGED)
    ListResponse<Edit> list(String domainType, UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        if (params.sort == null) {
            params.sort = 'dateCreated'
        }
        editRepository.readListResponseByMultiFacetAwareItemId(administeredItem.id, params)
    }

    @Audit
    @Operation(operationId = 'showEdit', summary = "Get an edit", description = "Returns an edit. You must have read privileges on the item in question.")
    @Get(Paths.EDIT_ID)
    Edit show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Edit validEdit = super.validateAndGet(domainType, domainId, id) as Edit
        validEdit
    }

    @Override
    @Audit(deletedObjectDomainType = Edit)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteEdit', summary = "Delete an edit", description = "Deletes an edit.")
    @Delete(Paths.EDIT_ID)
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        super.delete(id)
    }

    @Override
    Edit createEntity(@NonNull AdministeredItem administeredItem, @NonNull Edit edit) {
        if (!accessControlService.enabled || accessControlService.user == null) {return edit}
        super.createEntity(administeredItem, edit)
    }

    Edit createEdit(@NonNull AdministeredItem administeredItem, @NonNull EditType editType, @NonNull String theDescription) {
        final Edit edit = new Edit().tap {
            title = editType
            description = theDescription
        }
        createEntity(administeredItem, edit)
    }
}
