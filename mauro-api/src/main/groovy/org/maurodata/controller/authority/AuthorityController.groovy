package org.maurodata.controller.authority

import org.maurodata.audit.Audit
import org.maurodata.domain.facet.EditType

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
    @Get(Paths.AUTHORITY_ID)
    Authority show(@NonNull UUID id) {
        accessControlService.checkAuthenticated()
        authorityService.find(id)
    }

    @Audit
    @Get(Paths.AUTHORITY_LIST)
    ListResponse<Authority> list() {
        accessControlService.checkAuthenticated()
        ListResponse.from(authorityService.findAll())
    }

    @Audit
    @Transactional
    @Post(Paths.AUTHORITY_LIST)
    Authority create(@Body @NonNull Authority authority) {
        accessControlService.checkAdministrator()
        Authority cleanedItem = cleanBody(authority)
        cleanedItem = updateCreationProperties(cleanedItem) as Authority
        authorityService.create(cleanedItem)
    }

    @Audit
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
