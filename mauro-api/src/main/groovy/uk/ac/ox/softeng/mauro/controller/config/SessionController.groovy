package uk.ac.ox.softeng.mauro.controller.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.security.AccessControlService

@CompileStatic
@Slf4j
@Controller('/session')
@Secured(SecurityRule.IS_ANONYMOUS)
class SessionController {

    @Inject
    AccessControlService accessControlService

    @Get('/isAuthenticated')
    Map<String, Boolean> isAuthenticated() {
        [
            authenticatedSession: accessControlService.isUserAuthenticated()
        ]
    }
    @Get('/isApplicationAdministration')
    Map<String, Boolean> isApplicationAdministration() {
        [
            authenticatedSession: accessControlService.isAdministrator()
        ]
    }
}
