package org.maurodata.controller.security.tracking

import org.maurodata.api.Paths

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@Filter("/**")
@Singleton
class AccessTrackingFilter implements HttpServerFilter {

    private final SessionTracker tracker
    private final SecurityService securityService

    @Inject
    AccessTrackingFilter(SessionTracker tracker, @Nullable SecurityService securityService) {
        this.tracker = tracker
        this.securityService = securityService
    }

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                               ServerFilterChain chain) {
        if (request.getPath().startsWith(Paths.SESSION_ADMIN_ACTIVE_SESSIONS) ||
            request.getPath().startsWith(Paths.API_PROPERTY_LIST_ALL)
        ) {
            return chain.proceed(request)
        }

        return Mono.from(chain.proceed(request))
            .doOnSuccess(response -> {
                request.getCookies().findCookie("SESSION").ifPresent(cookie -> {
                    String sessionId = cookie.getValue()
                    String path = request.getPath()

                    if (securityService != null) {
                        securityService.getAuthentication().ifPresentOrElse(
                            auth -> {
                                String username = auth.getName()
                                tracker.updateSession(sessionId, username, path)
                            }
                            ,
                            () -> {
                                tracker.updateSession(sessionId, "", path)
                            }
                        )
                    } else {
                        tracker.updateSession(sessionId, "", path)
                    }
                })
            })
    }
}
