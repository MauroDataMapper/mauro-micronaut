package uk.ac.ox.softeng.mauro.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.security.AccessControlService

@Singleton
@Slf4j
class MauroSessionLoginHandler extends SessionLoginHandler {

    @Inject
    AccessControlService accessControlService

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore, @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService)
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        MutableHttpResponse defaultResponse = super.loginSuccess(authentication, request)
        if (defaultResponse == HttpResponse.ok()) {
            log.debug 'Successful login, returning Authentication'
            return HttpResponse.ok(authentication)
        } else {
            defaultResponse
        }
    }
}
