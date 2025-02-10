package uk.ac.ox.softeng.mauro.api

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.ClientFilter
import io.micronaut.http.annotation.RequestFilter
import io.micronaut.http.annotation.ResponseFilter
import io.micronaut.http.cookie.Cookie
import jakarta.inject.Singleton

@ClientFilter
@Singleton
@Slf4j
class SessionHandlerClientFilter {

    String sessionId

    HttpStatus lastStatus

    @RequestFilter
    void doFilter(MutableHttpRequest<?> request) {
        log.trace("Applying request filter: ${this.class}")
        if(sessionId) {
            request.cookie(Cookie.of('SESSION', sessionId))
        }

    }

    @ResponseFilter
    void sessionResponse(HttpResponse<?> response) {
        log.trace("Applying response filter: ${this.class}")
        log.trace("${response.status()}")
        lastStatus = response.status()
        log.trace("${response.body()}")
        Optional<Cookie> sessionCookie = response.getCookie('SESSION')
        if(sessionCookie) {
            sessionId = sessionCookie.get().value
        }

    }
}
