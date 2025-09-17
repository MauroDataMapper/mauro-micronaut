package org.maurodata.controller.config

import org.maurodata.api.Paths
import org.maurodata.api.config.SessionApi
import org.maurodata.audit.Audit
import org.maurodata.controller.security.tracking.SessionTracker
import org.maurodata.controller.security.tracking.TrackedSession
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.security.CatalogueUserRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.security.AccessControlService

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SessionController implements SessionApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    SessionTracker sessionTracker

    @Inject
    CatalogueUserRepository catalogueUserRepository

    @Audit
    @Get(Paths.SESSION_IS_AUTHENTICATED)
    Map<String, Boolean> isAuthenticated() {
        [
            authenticatedSession: accessControlService.isUserAuthenticated()
        ]
    }

    @Audit
    @Get(Paths.SESSION_IS_APP_ADMIN)
    Map<String, Boolean> isApplicationAdministration() {
        [
            applicationAdministrationSession: accessControlService.isAdministrator()
        ]
    }

    @Audit
    @Get(Paths.SESSION_AUTH_DETAILS)
    Map authenticationDetails(@Nullable Authentication authentication) {
        [
            isAuthenticated: authentication as Boolean,
            attributes: authentication?.attributes
        ]
    }

    @Audit
    @Get(Paths.SESSION_CHECK_AUTHENTICATED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    String checkAuthenticated() {
        'Authenticated'
    }

    @Audit
    @Get(Paths.SESSION_CHECK_ANONYMOUS)
    String checkAnonymous() {
        'Anonymous'
    }

    @Audit
    @Override
    @Get(Paths.SESSION_ADMIN_ACTIVE_SESSIONS)
    Map activeSessions() {
        Collection<TrackedSession> trackedSessions = sessionTracker.activeSessions

        List<Map> items=[]
        List<Map> authorisedItems=[]
        List<Map> unauthorisedItems=[]

        trackedSessions.forEach { TrackedSession trackedSession ->

            CatalogueUser catalogueUser = null

            if(trackedSession.userEmailAddress != null && !trackedSession.userEmailAddress.isEmpty() && trackedSession.userEmailAddress != TrackedSession.UNLOGGED_USER_EMAIL) {
                catalogueUser = catalogueUserRepository.readByEmailAddress(trackedSession.userEmailAddress)
            }

            LinkedHashMap item = [
                id: trackedSession.id,
                lastAccessedDateTime : trackedSession.lastAccessedDateTime,
                creationDateTime : trackedSession.creationDateTime,
                userEmailAddress : trackedSession.userEmailAddress?trackedSession.userEmailAddress:TrackedSession.UNLOGGED_USER_EMAIL,
                userName: catalogueUser?catalogueUser.fullName:null,
                userOrganisation: catalogueUser?catalogueUser.organisation:null,
                lastAccessedUrl: trackedSession.lastAccessedUrl
            ]

            items.add(item)
            if(catalogueUser!=null){
                authorisedItems.add(item)
            } else {
                unauthorisedItems.add(item)
            }
        }

        Map<String,Object> activeSessions = [

            countAuthorised: authorisedItems.size(),
            countUnauthorised: unauthorisedItems.size(),
            items: items,
            authorisedItems: authorisedItems,
            unauthorisedItems : unauthorisedItems
        ]

        return activeSessions
    }
}
