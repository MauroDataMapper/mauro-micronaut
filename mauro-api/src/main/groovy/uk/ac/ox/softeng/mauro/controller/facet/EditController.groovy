package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.EditApi
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class EditController implements EditApi {

    // todo: Stub method that needs implementing
    @Override
    @Get(Paths.EDIT_LIST)
    ListResponse list(String domainType, UUID domainId) {
        ListResponse.from([])
    }

}