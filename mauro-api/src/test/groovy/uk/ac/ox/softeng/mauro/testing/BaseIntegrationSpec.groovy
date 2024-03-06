package uk.ac.ox.softeng.mauro.testing

import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import spock.lang.Specification

class BaseIntegrationSpec extends Specification {
    public static final String FOLDERS_PATH = '/folders'
    public static final String TERMINOLOGIES_PATH = "/terminologies"
    public static final String TERMS_PATH = "/terms"
    public static final String CODE_SET_PATH = "/codeSets"

    @Inject
    @Client('/')
    HttpClient client

    Object GET(String uri) {
        client.toBlocking().retrieve(HttpRequest.GET(uri), Map<String, Object>)
    }

    <T> T GET(String uri, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.GET(uri), type)
    }
    Map<String, Object> POST(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    Map<String, Object> POST(String uri, MultipartBody body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE), Map<String, Object>)
    }

    <T> T POST(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), type)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), Map<String, Object>)
    }

    <T> T PUT(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), type)
    }

    Map<String, Object> DELETE(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), Map<String, Object>)
    }

    <T> T DELETE(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), type)
    }

    <T> T DELETE(String uri, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri), type)
    }
}
