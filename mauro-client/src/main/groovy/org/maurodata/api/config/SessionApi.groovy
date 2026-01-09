package org.maurodata.api.config

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.security.authentication.Authentication


@MauroApi
interface SessionApi {

    @Get(Paths.SESSION_IS_AUTHENTICATED)
    Map<String, Boolean> isAuthenticated()

    @Get(Paths.SESSION_IS_APP_ADMIN)
    Map<String, Boolean> isApplicationAdministration()

    @Get(Paths.SESSION_AUTH_DETAILS)
    Map authenticationDetails(@Nullable Authentication authentication)

    @Get(Paths.SESSION_CHECK_AUTHENTICATED)
    String checkAuthenticated()

    @Get(Paths.SESSION_CHECK_ANONYMOUS)
    String checkAnonymous()

    @Get(Paths.SESSION_ADMIN_ACTIVE_SESSIONS)
    Map activeSessions()
}
