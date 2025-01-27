package org.maurodata

import groovy.util.logging.Slf4j
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.ClientFilter
import io.micronaut.http.annotation.RequestFilter
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import jakarta.inject.Singleton

@ClientFilter
@Singleton
@Slf4j
class TestApiClientFilter {

    @Inject
    EmbeddedServer embeddedServer

    @RequestFilter
    void doFilter(MutableHttpRequest<?> request) {
        log.trace("Applying request filter: ${this.class}")
        UriBuilder builder = UriBuilder.of(request.getUri())
        builder.host(embeddedServer.host)
        builder.port(embeddedServer.port)
        request.uri (builder.build())
    }
}
