package org.maurodata.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.*
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.endpoints.LoginControllerConfigurationProperties
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import org.maurodata.security.AccessControlService

@Singleton
@Slf4j
@Replaces(SessionLoginHandler)
class MauroSessionLoginHandler extends SessionLoginHandler {

    @Inject
    AccessControlService accessControlService

    @Inject
    CatalogueUserCacheableRepository catalogueUserCacheableRepository

    @Inject
    LoginControllerConfigurationProperties loginControllerConfigurationProperties

    @Value('${mauro.oauth.login-success:/}')
    URI loginSuccessUrl

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore, @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService)
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        log.debug 'At MauroSessionLoginHandler loginSuccess!'
        MutableHttpResponse defaultResponse = super.loginSuccess(authentication, request)
        if (defaultResponse.status == HttpStatus.OK) {
            if (request.path == loginControllerConfigurationProperties.path) {
                log.debug 'Successful login, returning Authentication'
                return HttpResponse.ok(catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id))
            } else {
                log.debug 'Successful login, redirecting to login success URI'
                MutableHttpResponse<?> response = HttpResponse.status(HttpStatus.SEE_OTHER)
                MutableHttpHeaders headers = response.headers
                headers.location(loginSuccessUrl)
                response
            }
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
