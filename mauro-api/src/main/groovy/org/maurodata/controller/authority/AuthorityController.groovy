package org.maurodata.controller.authority

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.audit.Audit

import io.micronaut.http.HttpResponse
import org.maurodata.api.Paths
import org.maurodata.api.authority.AuthorityApi
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.authority.Authority
import org.maurodata.service.core.AuthorityService
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class AuthorityController extends ItemController<Authority> implements AuthorityApi {
    final AuthorityService authorityService

    @Inject
    AuthorityController(AuthorityService authorityService) {
        super(authorityService.authorityRepository)
        this.authorityService = authorityService
    }

    @Audit
    @Operation(summary = "Get an authority", description = "Returns an authority. It is available to authenticated users.")
    @Get(Paths.AUTHORITY_ID)
    Authority show(@NonNull UUID id) {
        accessControlService.checkAuthenticated()
        authorityService.find(id)
    }

    @Audit
    @Operation(operationId = 'listAuthority', summary = "List the authorities", description = "Returns the authorities. It is available to authenticated users.")
    @Get(Paths.AUTHORITY_LIST)
    ListResponse<Authority> list() {
        accessControlService.checkAuthenticated()
        ListResponse.from(authorityService.findAll())
    }

    @Audit
    @Transactional
    @Operation(operationId = 'createAuthority', summary = "Create an authority", description = "Creates an authority. It is only available to administrator users.")
    @Post(Paths.AUTHORITY_LIST)
    Authority create(@Body @NonNull Authority authority) {
        accessControlService.checkAdministrator()
        Authority cleanedItem = cleanBody(authority)
        cleanedItem = updateCreationProperties(cleanedItem) as Authority
        authorityService.create(cleanedItem)
    }

    @Audit
    @Operation(operationId = 'updateAuthority',summary = "Update an authority", description = "Updates an authority. It is only available to administrator users.")
    @Put(Paths.AUTHORITY_ID)
    Authority update(UUID id, @Body @NonNull Authority authority) {
        accessControlService.checkAdministrator()
        Authority cleanItem = cleanBody(authority)
        Authority existing = authorityService.readById(id)

        boolean hasChanged = updateProperties(existing, cleanItem)
        if (hasChanged) {
            return authorityService.update(existing)
        } else {
            return null
        }
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete an authority", description = "Deletes an authority. It is only available to administrator users.")
    @Delete(Paths.AUTHORITY_ID)
    HttpResponse delete(UUID id, @Body @Nullable Authority authority) throws HttpStatusException {
        accessControlService.checkAdministrator()

        Authority authorityToDelete = authorityService.readById(id)
        if (authorityToDelete?.version) authorityToDelete.version = authority.version
        Long deleted = authorityService.delete(authorityToDelete)
        if (deleted) {
            return HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }
}
