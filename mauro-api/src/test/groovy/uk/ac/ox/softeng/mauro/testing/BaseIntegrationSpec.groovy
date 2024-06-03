package uk.ac.ox.softeng.mauro.testing

import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

class BaseIntegrationSpec extends Specification {
    public static final String FOLDERS_PATH = '/folders'
    public static final String TERMINOLOGIES_PATH = "/terminologies"
    public static final String TERMS_PATH = "/terms"
    public static final String CODE_SET_PATH = "/codeSets"
    public static final String METADATA_PATH = '/metadata'
    public static final String SUMMARY_METADATA_PATH = '/summaryMetadata'
    public static final String SUMMARY_METADATA_REPORT_PATH = '/summaryMetadataReports'
    public static final String ANNOTATION_PATH = '/annotations'
    public static final String DATAMODELS_PATH = '/dataModels'
    public static final String DATACLASSES_PATH = '/dataClasses'
    public static final String DATATYPES_PATH = '/dataTypes'
    public static final String DATA_ELEMENTS_PATH = '/dataElements'
    public static final String EXPORT_PATH = '/export'
    public static final String IMPORT_PATH = '/import'
    public static final String AUTHOR = 'author'
    public static final String PATH_IDENTIFIER = 'pathIdentifier'
    public static final String MODEL_VERSION_TAG = 'modelVersionTag'
    public static final String FINALISED = 'finalised'
    public static final String DATE_FINALISED = 'dateFinalised'
    public static final String TERM_RELATIONSHIP_TYPES = "/termRelationshipTypes"


    @Inject
    @Client('/')
    @Shared
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

    Map<String, Object> POST(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), Map<String, Object>)
    }

    Map<String, Object> POST(String uri, MultipartBody body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE), Map<String, Object>)
    }

    <T> T POST(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), type)
    }

    <T> T POST(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body), type)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), Map<String, Object>)
    }

    Map<String, Object> PUT(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), Map<String, Object>)
    }

    <T> T PUT(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), type)
    }

    <T> T PUT(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body), type)
    }

    Map<String, Object> DELETE(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), Map<String, Object>)
    }

    <T> T DELETE(String uri, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri), type)
    }

    <T> T DELETE(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), type)
    }

    <T> T DELETE(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body), type)
    }
}
