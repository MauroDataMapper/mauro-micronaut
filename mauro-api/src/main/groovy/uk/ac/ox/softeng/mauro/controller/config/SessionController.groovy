package uk.ac.ox.softeng.mauro.controller.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
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
            applicationAdministrationSession: accessControlService.isAdministrator()
        ]
    }

    @Get('/authenticationDetails')
    Map authenticationDetails(@Nullable Authentication authentication) {
        [
            isAuthenticated: authentication as Boolean,
            attributes: authentication?.attributes
        ]
    }

    @Get('/checkAuthenticated')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    String checkAuthenticated() {
        'Authenticated'
    }

    @Get('/checkAnonymous')
    String checkAnonymous() {
        'Anonymous'
    }
}
