package org.maurodata.testing

import io.micronaut.http.MutableHttpRequest
import org.maurodata.plugin.importer.json.JsonDataModelImporterPlugin
import org.maurodata.plugin.importer.json.JsonTerminologyImporterPlugin
import org.maurodata.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.cookie.Cookie
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

// This class is mostly no-longer required, but we'll keep the methods, in case we need them in the future.
class BaseIntegrationSpec extends Specification {

    @Inject
    ObjectMapper objectMapper

    @Inject
    JsonDataModelImporterPlugin jsonDataModelImporterPlugin

    @Inject
    JsonTerminologyImporterPlugin jsonTerminologyImporterPlugin

    @Inject
    @Client('/')
    @Shared
    HttpClient client

    @Shared
    Cookie sessionCookie

    <T> T createRequest(MutableHttpRequest request, Class<T> type) {
        if (sessionCookie) {
            request.cookie(sessionCookie)
        }
        client.toBlocking().retrieve(request, type)
    }


    Map<String, Object> GET(String uri) {
        createRequest(HttpRequest.GET(uri), Map<String, Object>)
    }

    <T> T GET(String uri, Class<T> type, Class internalType = null) {
        def response = createRequest(HttpRequest.GET(uri), type)
        if(type == ListResponse && internalType) {
            ((ListResponse) response).bindItems(objectMapper, internalType)
        }
        // TODO: Do something with List type here
        return response
    }

    Map<String, Object> POST(String uri, Map<String, Object> body) {
        createRequest(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    Map<String, Object> POST(String uri, Object body) {
        createRequest(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    Map<String, Object> POST(String uri, MultipartBody body) {
        createRequest(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    <T> T POST(String uri, Map<String, Object> body, Class<T> type) {
        createRequest(HttpRequest.POST(uri, body), type)
    }

    <T> T POST(String uri, Object body, Class<T> type) {
        createRequest(HttpRequest.POST(uri, body), type)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        createRequest(HttpRequest.PUT(uri, body), Map<String, Object>)
    }

    Map<String, Object> PUT(String uri, Object body) {
        createRequest(HttpRequest.PUT(uri, body), Map<String, Object>)
    }

    <T> T PUT(String uri, Map<String, Object> body, Class<T> type) {
        createRequest(HttpRequest.PUT(uri, body), type)
    }

    <T> T PUT(String uri, Object body, Class<T> type) {
        createRequest(HttpRequest.PUT(uri, body), type)
    }

    Map<String, Object> DELETE(String uri) {
        createRequest(HttpRequest.DELETE(uri), Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Map<String, Object> body) {
        createRequest(HttpRequest.DELETE(uri, body), Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Object body) {
        createRequest(HttpRequest.DELETE(uri, body), Map<String, Object>)
    }

    <T> T DELETE(String uri, Class<T> type) {
        createRequest(HttpRequest.DELETE(uri), type)
    }

    <T> T DELETE(String uri, Map<String, Object> body, Class<T> type) {
        createRequest(HttpRequest.DELETE(uri, body), type)
    }

    <T> T DELETE(String uri, Object body, Class<T> type) {
        createRequest(HttpRequest.DELETE(uri, body), type)
    }

}