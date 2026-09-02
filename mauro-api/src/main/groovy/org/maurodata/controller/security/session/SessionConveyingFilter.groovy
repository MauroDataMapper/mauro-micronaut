package org.maurodata.controller.security.session

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.util.CollectionUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.filter.ServerFilterPhase
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import io.micronaut.session.http.HttpSessionConfiguration
import io.micronaut.session.http.HttpSessionFilter
import io.micronaut.session.http.HttpSessionIdResolver
import io.micronaut.session.http.SessionForRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.reactivestreams.Publisher

@CompileStatic
@Filter("/**")
@Singleton
@Slf4j
class SessionConveyingFilter implements HttpServerFilter {

    public static final Integer ORDER = ServerFilterPhase.SESSION.order() + 10

    @Inject
    private final SessionStore<Session> sessionStore

    @Inject
    private final HttpSessionConfiguration configuration

    @Inject
    private final HttpSessionIdResolver[] resolvers

    @Override
    int getOrder() {
        return ORDER
    }

    @Inject
    SessionConveyingFilter(SessionStore<Session> sessionStore, HttpSessionConfiguration configuration, HttpSessionIdResolver[] resolvers) {
        this.sessionStore = sessionStore
        this.configuration = configuration
        this.resolvers = resolvers
    }

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                               ServerFilterChain chain) {
        request.setAttribute(SessionConveyingFilter.getName(), true)

        try {
            request.getCookies().findCookie(configuration.getCookieName()).ifPresent(cookie -> {

                Session sessionIsThere = SessionForRequest.find(request).orElse(null)
                if (!sessionIsThere) {
                    for (HttpSessionIdResolver resolver : resolvers) {
                        List<String> ids = resolver.resolveIds(request)
                        if (CollectionUtils.isNotEmpty(ids)) {
                            String sessionId = ids.get(0)
                            // Not retrievable, pull the session directly from the store
                            sessionStore.findSession(sessionId).thenAccept(optionalSession -> {
                                if (optionalSession.isPresent()) {
                                    Session session = optionalSession.get()
                                    if (!session.isExpired()) {
                                        request.getAttributes().put(
                                            HttpSessionFilter.SESSION_ATTRIBUTE, session
                                        )
                                    }
                                }
                            })
                        }
                    }
                }
            })
        } catch (Throwable th) {
            th.printStackTrace()
        }

        return chain.proceed(request)
    }
}
