package uk.ac.ox.softeng.mauro.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

@Singleton
@Slf4j
@Replaces(SessionLoginHandler)
@Named('Session')
class MauroSessionLoginHandler extends SessionLoginHandler {

    @Inject
    AccessControlService accessControlService

    @Inject
    CatalogueUserCacheableRepository catalogueUserCacheableRepository

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore, @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService)
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        MutableHttpResponse defaultResponse = super.loginSuccess(authentication, request)
        if (defaultResponse.status == HttpStatus.OK) {
            log.debug 'Successful login, returning Authentication'
            return HttpResponse.ok(catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id))
        } else {
            defaultResponse
        }
    }

    @Override
    MutableHttpResponse<?> loginFailed(AuthenticationResponse authenticationFailed, HttpRequest<?> request) {
        MutableHttpResponse defaultResponse = super.loginFailed(authenticationFailed, request)
        if (defaultResponse.status == HttpStatus.OK) {
            log.debug 'Login failed'
            return HttpResponse.unauthorized()
        } else {
            defaultResponse
        }
    }

}
