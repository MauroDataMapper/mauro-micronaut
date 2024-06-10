package uk.ac.ox.softeng.mauro.testing

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonDataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonTerminologyImporterPlugin

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

    @Inject
    JsonDataModelImporterPlugin jsonDataModelImporterPlugin

    @Inject
    JsonTerminologyImporterPlugin jsonTerminologyImporterPlugin

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
            terminology  terminologyToImport
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
