package uk.ac.ox.softeng.mauro.testing

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Inject
import spock.lang.Specification

class BaseIntegrationSpec extends Specification {

    @Inject
    @Client('/')
    HttpClient client

    Object GET(String uri, Class type = Map<String, Object>) {
        client.toBlocking().retrieve(HttpRequest.GET(uri), type)
    }

    Map<String, Object> POST(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), Map<String, Object>)
    }
}
