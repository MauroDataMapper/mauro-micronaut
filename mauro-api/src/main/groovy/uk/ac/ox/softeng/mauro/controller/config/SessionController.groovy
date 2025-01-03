package uk.ac.ox.softeng.mauro.controller.config

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.config.SessionApi

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
class SessionController implements SessionApi {

    @Inject
    AccessControlService accessControlService

    @Get(Paths.SESSION_IS_AUTHENTICATED)
    Map<String, Boolean> isAuthenticated() {
        [
            authenticatedSession: accessControlService.isUserAuthenticated()
        ]
    }

    @Get(Paths.SESSION_IS_APP_ADMIN)
    Map<String, Boolean> isApplicationAdministration() {
        [
            applicationAdministrationSession: accessControlService.isAdministrator()
        ]
    }

    @Get(Paths.SESSION_AUTH_DETAILS)
    Map authenticationDetails(@Nullable Authentication authentication) {
        [
            isAuthenticated: authentication as Boolean,
            attributes: authentication?.attributes
        ]
    }

    @Get(Paths.SESSION_CHECK_AUTHENTICATED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    String checkAuthenticated() {
        'Authenticated'
    }

    @Get(Paths.SESSION_CHECK_ANONYMOUS)
    String checkAnonymous() {
        'Anonymous'
    }
}
