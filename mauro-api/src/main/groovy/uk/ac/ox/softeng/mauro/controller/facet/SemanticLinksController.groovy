package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_ANONYMOUS)
class SemanticLinksController {

    //todo: implement actual
    @Get(Paths.SEMANTIC_LINKS_LIST)
    ListResponse list(@NonNull String domainType, @NonNull UUID domainId) {
        ListResponse.from([])
    }

}
