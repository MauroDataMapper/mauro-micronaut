package org.maurodata.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.*
import io.micronaut.http.context.ServerContextPathProvider
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.cookie.Cookies
import io.micronaut.http.server.util.HttpHostResolver
import io.micronaut.http.uri.UriBuilder
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.endpoints.LoginControllerConfigurationProperties
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.filters.SecurityFilter
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.security.session.SessionPopulator
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import io.micronaut.session.http.HttpSessionConfiguration
import io.micronaut.session.http.HttpSessionFilter
import io.micronaut.session.http.HttpSessionIdEncoder
import io.micronaut.session.http.SessionForRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import org.maurodata.security.AccessControlService

import java.time.Duration

@CompileStatic
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

    @Inject
    ServerContextPathProvider serverContextPathProvider

    @Inject
    HttpHostResolver httpHostResolver

    @Inject
    HttpSessionConfiguration configuration

    @Inject
    HttpSessionIdEncoder[] encoders

    @Inject
    List<SessionPopulator<HttpRequest<?>>> sessionPopulators

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore,
                             @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService,
                             List<SessionPopulator<HttpRequest<?>>> sessionPopulators) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService, sessionPopulators)
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        log.debug 'At MauroSessionLoginHandler loginSuccess!'
        MutableHttpResponse defaultResponse = super.loginSuccess(authentication, request)

        // Session to expire and remove
        Session session = SessionForRequest.find(request).get()
        // Remember cookies to preserve UI_REDIRECT_URL
        Cookies requestCookies = request.getCookies()

        if (session != null) {

            log.debug("Existing session ${session.isNew()}")

            if (session.isNew()) {
                sessionStore.save(session)
                    .exceptionally(ex -> {
                        log.error("Failed to save session", ex)
                        return null
                    })

                for (HttpSessionIdEncoder encoder : encoders) {
                    encoder.encodeId(request, defaultResponse, session)
                }
            } else {
                // Create a new session and retire the session that led to this login
                // and then tidy-up

                session.setMaxInactiveInterval(Duration.ZERO)
                sessionStore.deleteSession(session.id)

                session.remove(SecurityFilter.AUTHENTICATION)

                Session loginSession = SessionForRequest.create(sessionStore, request)
                request.setAttribute(HttpSessionFilter.SESSION_ATTRIBUTE, loginSession)
                sessionPopulators.forEach(sessionPopulator -> sessionPopulator.populateSession(request, authentication, loginSession))

                sessionStore.save(loginSession)
                    .exceptionally(ex -> {
                        log.error("Failed to save session", ex)
                        return null
                    })

                MutableHttpHeaders headers = defaultResponse.getHeaders() as MutableHttpHeaders
                final String[] headerNames = configuration.getHeaderNames()
                headers.remove(HttpHeaders.SET_COOKIE)
                headers.remove(headerNames[0])
                headers.add(headerNames[0], loginSession.getId())

                for (HttpSessionIdEncoder encoder : encoders) {
                    encoder.encodeId(request, defaultResponse, loginSession)
                }
            }
        }

        if (defaultResponse.status == HttpStatus.OK) {
            String contextPath = serverContextPathProvider.getContextPath() ?: ""

            String loginPath = UriBuilder.of("/")
                .path(contextPath)
                .path(loginControllerConfigurationProperties.path)
                .build().toString()

            if (request.path == loginPath) {
                log.debug 'Successful login, returning Authentication'
                return defaultResponse.body(catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id))
            } else {
                String configuredAppLoginSuccessURL = authentication.attributes.get('app-login-success') as String
                Optional<Cookie> cookieAppLoginSuccessURL = requestCookies.findCookie(OAuthRedirectFilter.UI_REDIRECT_URL)

                URI appLoginSuccess = null
                try {
                    appLoginSuccess =
                        cookieAppLoginSuccessURL.present ? new URI(cookieAppLoginSuccessURL.get().value) :
                        configuredAppLoginSuccessURL ? new URI(configuredAppLoginSuccessURL) :
                        loginSuccessUrl
                } catch (Exception e) {
                    if (cookieAppLoginSuccessURL.present) {
                        log.warn("App login success redirect: Invalid value for Cookie '${OAuthRedirectFilter.UI_REDIRECT_URL}'")
                    } else {
                        log.warn("App login success redirect: Invalid value for Configured '${configuredAppLoginSuccessURL}'")
                    }
                }

                if (appLoginSuccess) {
                    log.debug 'Successful login, redirecting to login success URI'
                    MutableHttpResponse<?> response = defaultResponse.status(HttpStatus.SEE_OTHER)
                    MutableHttpHeaders headers = response.headers as MutableHttpHeaders
                    headers.location(appLoginSuccess)
                    return response
                }
            }
        }

        defaultResponse
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
