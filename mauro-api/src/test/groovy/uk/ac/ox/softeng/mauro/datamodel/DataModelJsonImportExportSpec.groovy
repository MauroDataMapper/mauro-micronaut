package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
class DataModelJsonImportExportSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Inject
    ObjectMapper objectMapper

    @Shared
    UUID folderId

    @Shared
    ExportModel exportModel

    @Shared
    UUID dataModelId

    JsonSlurper jsonSlurper = new JsonSlurper()

    void 'create dataModel and export'() {
        given:
        folderId = UUID.fromString(POST("$FOLDERS_PATH", [label: 'Test folder']).id)
        dataModelId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model']).id)
        UUID dataClass1Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class']).id)
        UUID dataClass2Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-2', definition: 'second data class']).id)
        UUID dataTypeId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'Test data type', domainType: 'PrimitiveType']).id)
        UUID dataElementId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClass1Id$DATA_ELEMENTS_PATH",
                dataElementPayload('dataElementLabel', dataTypeId)).id)
        UUID summaryMetadataId = UUID.fromString(POST("$DATA_ELEMENTS_PATH/$dataElementId$SUMMARY_METADATA_PATH", summaryMetadataPayload()).id)
        UUID summaryMetadataReportId = UUID.fromString(POST("$DATA_ELEMENTS_PATH/$dataElementId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH", summaryMetadataReport()).id)

        when:
        String json = GET("$DATAMODELS_PATH/$dataModelId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE/JsonDataModelExporterPlugin$JSON_EXPORTER_VERSION", String)

        then:
        json
        Map parsedJson = jsonSlurper.parseText(json) as Map
        parsedJson.exportMetadata
        parsedJson.dataModel.id == dataModelId.toString()
        parsedJson.dataModel.label == 'Test data model'
        parsedJson.dataModel.dataClasses.size() == 2
        parsedJson.dataModel.dataClasses.dataElements.flatten().id[0] == dataElementId.toString()
        parsedJson.dataModel.dataClasses.dataElements.summaryMetadata.flatten().id[0] == summaryMetadataId.toString()
        parsedJson.dataModel.dataClasses.dataElements.summaryMetadata.summaryMetadataReports.flatten().id[0] == summaryMetadataReportId.toString()
    }

    void 'import dataModel and verify'() {
        given:
        exportModel = GET("$DATAMODELS_PATH/$dataModelId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE/JsonDataModelExporterPlugin$JSON_EXPORTER_VERSION", ExportModel)

        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()
        def response = POST('/dataModels/import/uk.ac.ox.softeng.mauro.plugin.importer.json/JsonDataModelImporterPlugin/4.0.0', importRequest)

        when:
        UUID importedDataModelId = UUID.fromString(response.items.first().id)
        def dataModel = GET("/dataModels/$importedDataModelId")

        then:
        dataModel.path == 'dm:Test data model$main'

        when:
        def dataClasses = GET("/dataModels/$importedDataModelId/dataClasses")

        then:
        dataClasses.items.path.sort() == ['dm:Test data model$main|dc:TEST-1', 'dm:Test data model$main|dc:TEST-2']

        when:
        def dataTypes = GET("/dataModels/$importedDataModelId/dataTypes")

        then:
        dataTypes.items.path == ['dm:Test data model$main|dt:Test data type']

    }
}
