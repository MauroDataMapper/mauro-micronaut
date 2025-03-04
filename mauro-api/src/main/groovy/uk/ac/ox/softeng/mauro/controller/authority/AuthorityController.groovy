package uk.ac.ox.softeng.mauro.controller.authority

import uk.ac.ox.softeng.mauro.Paths
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.service.core.AuthorityService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
@Slf4j
@Controller(Paths.AUTHORITIES_ROUTE)
@Secured(SecurityRule.IS_ANONYMOUS)
class AuthorityController extends ItemController<Authority> {
    final AuthorityService authorityService

    @Inject
    AuthorityController(AuthorityService authorityService) {
        super(authorityService.authorityRepository)
        this.authorityService = authorityService
    }

    @Get(value = Paths.ID_ROUTE)
    Authority show(@NonNull UUID id) {
        accessControlService.checkAuthenticated()
        authorityService.find(id)
    }

    @Get
    ListResponse<Authority> list() {
        accessControlService.checkAuthenticated()
        ListResponse.from(authorityService.findAll())
    }

    @Post
    @Transactional
    Authority create(@Body @NonNull Authority authority) {
        accessControlService.checkAdministrator()
        Authority cleanedItem = cleanBody(authority)
        cleanedItem = updateCreationProperties(cleanedItem) as Authority
        authorityService.create(cleanedItem)
    }

    @Put(value = Paths.ID_ROUTE)
    Authority update(UUID id, @Body @NonNull Authority authority) {
        accessControlService.checkAdministrator()
        Authority cleanItem = cleanBody(authority)
        Authority existing = authorityService.readById(id)

        boolean hasChanged = updateProperties(existing, cleanItem)
        if (hasChanged) {
            authorityService.update(existing)
        }
    }


    @Transactional
    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(UUID id, @Body @Nullable Authority authority) {
        accessControlService.checkAdministrator()

        Authority authorityToDelete = authorityService.readById(id)
        if (authorityToDelete?.version) authorityToDelete.version = authority.version
        Long deleted = authorityService.delete(authorityToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }
}
