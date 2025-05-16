package uk.ac.ox.softeng.mauro.testing

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonDataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonTerminologyImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.cookie.Cookie
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
    public static final String DATA_FLOWS_PATH = '/dataFlows'
    public static final String EXPORT_PATH = '/export'
    public static final String IMPORT_PATH = '/import'
    public static final String AUTHOR = 'author'
    public static final String PATH_IDENTIFIER = 'pathIdentifier'
    public static final String PATH_MODEL_IDENTIFIER = 'pathModelIdentifier'
    public static final String MODEL_VERSION_TAG = 'modelVersionTag'
    public static final String FINALISED = 'finalised'
    public static final String DATE_FINALISED = 'dateFinalised'
    public static final String TERM_RELATIONSHIP_TYPES = "/termRelationshipTypes"
    public static final String TERM_RELATIONSHIP_PATH = "/termRelationships"
    public static final String JSON_EXPORTER_NAMESPACE = '/uk.ac.ox.softeng.mauro.plugin.exporter.json'
    public static final String JSON_EXPORTER_NAME = '/JsonFolderExporterPlugin'
    public static final String JSON_EXPORTER_VERSION = '/4.0.0'
    public static final String JSON_IMPORTER_NAMESPACE = '/uk.ac.ox.softeng.mauro.plugin.importer.json'
    public static final String JSON_IMPORTER_NAME = '/JsonFolderImporterPlugin'
    public static final String JSON_IMPORTER_VERSION = '/4.0.0'
    public static final String DATA_CLASS_COMPONENTS_PATH = '/dataClassComponents'
    public static final String DATA_ELEMENT_COMPONENTS_PATH = '/dataElementComponents'
    public static final String SOURCE = '/source'
    public static final String TARGET = '/target'
    public static final String REFERENCE_FILE_PATH = '/referenceFiles'
    public static final String CLASSIFICATION_SCHEME_PATH = '/classificationSchemes'
    public static final String CLASSIFIER_PATH = '/classifiers'
    public static final String NEW_BRANCH_MODEL_VERSION = '/newBranchModelVersion'
    public static final String DIFF = '/diff'

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

    Map<String, Object> GET(String uri) {
        client.toBlocking().retrieve(HttpRequest.GET(uri).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    <T> T GET(String uri, Class<T> type, Class internalType = null) {
        def response = client.toBlocking().retrieve(HttpRequest.GET(uri).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
        if(type == ListResponse && internalType) {
            ((ListResponse) response).bindItems(objectMapper, internalType)
        }
        // TODO: Do something with List type here
        response
    }

    Map<String, Object> POST(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    Map<String, Object> POST(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    Map<String, Object> POST(String uri, MultipartBody body) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    <T> T POST(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    <T> T POST(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.POST(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    Map<String, Object> PUT(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    Map<String, Object> PUT(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    <T> T PUT(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    <T> T PUT(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.PUT(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    Map<String, Object> DELETE(String uri) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Map<String, Object> body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    Map<String, Object> DELETE(String uri, Object body) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, Map<String, Object>)
    }

    <T> T DELETE(String uri, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    <T> T DELETE(String uri, Map<String, Object> body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
    }

    <T> T DELETE(String uri, Object body, Class<T> type) {
        client.toBlocking().retrieve(HttpRequest.DELETE(uri, body).tap { if (sessionCookie) it.cookie(sessionCookie) }, type)
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


}