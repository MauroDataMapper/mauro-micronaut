package org.maurodata.testing

import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.cookie.Cookie
import jakarta.inject.Inject
import spock.lang.Shared

class LowLevelApi {

    @Inject
    @Client('/')
    @Shared
    HttpClient client

    @Shared
    Cookie sessionCookie

    @Shared
    UUID apiKey

    Map<String, Object> GET(String uri) {
        client.toBlocking().retrieve(HttpRequest.GET(uri).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    <T> T GET(String uri, Class<T> type, Class internalType = null) {
        def response = client.toBlocking().retrieve(HttpRequest.GET(uri).tap {
            addHeaders(it)
        }, type)
        if(type == ListResponse && internalType) {
            ((ListResponse) response).bindItems(objectMapper, internalType)
        }
        // TODO: Do something with List type here
        response
    }

    Map<String, Object> POST(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    Map<String, Object> POST(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    Map<String, Object> POST(String uri, MultipartBody body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    <T> T POST(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap {
            addHeaders(it)
        }, type)
    }

    <T> T POST(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap {
            addHeaders(it)
        }, type)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    Map<String, Object> PUT(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    <T> T PUT(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap {
            addHeaders(it)
        }, type)
    }

    <T> T PUT(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap {
            addHeaders(it)
        }, type)
    }

    Map<String, Object> DELETE(String uri) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap {
            addHeaders(it)
        }, Map<String, Object>)
    }

    <T> T DELETE(String uri, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri).tap {
            addHeaders(it)
        }, type)
    }

    <T> T DELETE(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap {
            addHeaders(it)
        }, type)
    }

    <T> T DELETE(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap {
            addHeaders(it)
        }, type)
    }


    /**
     * Convenience method for importing a data model into the database for testing
     */

    UUID importDataModel(DataModel dataModelToImport, Folder folder) {
        ExportModel exportModel = ExportModel.build {
            dataModel dataModelToImport
        }
        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folder.id.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
                .build()
        String namespace = jsonDataModelImporterPlugin.namespace
        String name = jsonDataModelImporterPlugin.name
        String version = jsonDataModelImporterPlugin.version

        Map<String, Object> response = POST("/dataModels/import/$namespace/$name/$version", importRequest)
        UUID.fromString(response.items.first().id)
    }

    UUID importTerminology(Terminology terminologyToImport, Folder folder) {
        ExportModel exportModel = ExportModel.build {
            terminology terminologyToImport
        }
        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folder.id.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
                .build()
        String namespace = jsonTerminologyImporterPlugin.namespace
        String name = jsonTerminologyImporterPlugin.name
        String version = jsonTerminologyImporterPlugin.version

        Map<String, Object> response = POST("/terminologies/import/$namespace/$name/$version", importRequest)
        UUID.fromString(response.items.first().id)
    }


    void addHeaders(MutableHttpRequest<Object> request) {
        if(apiKey)
            request.header('apiKey', apiKey.toString())
        if (sessionCookie)
            request.cookie(sessionCookie)
    }


}
