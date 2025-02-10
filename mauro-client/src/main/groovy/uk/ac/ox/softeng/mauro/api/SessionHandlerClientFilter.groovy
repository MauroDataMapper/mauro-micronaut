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
        System.err.println("Applying response filter: ${this.class}")
        System.err.println(response.status())
        lastStatus = response.status()
        System.err.println(response.body())
        Optional<Cookie> sessionCookie = response.getCookie('SESSION')
        if(sessionCookie) {
            sessionId = sessionCookie.get().value
        }

    }
}
