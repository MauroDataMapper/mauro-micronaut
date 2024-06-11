package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

@ContainerizedTest
class DataModelJsonImportExportSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Inject
    ObjectMapper objectMapper

    @Shared
    UUID folderId

    @Shared
    ExportModel exportModel

    void 'create dataModel and export'() {
        given:
        folderId = UUID.fromString(POST('/folders', [label: 'Test folder']).id)
        UUID dataModelId = UUID.fromString(POST("/folders/$folderId/dataModels", [label: 'Test data model']).id)
        UUID dataClass1Id = UUID.fromString(POST("/dataModels/$dataModelId/dataClasses", [label: 'TEST-1', definition: 'first data class']).id)
        UUID dataClass2Id = UUID.fromString(POST("/dataModels/$dataModelId/dataClasses", [label: 'TEST-2', definition: 'second data class']).id)
        UUID dataTypeId = UUID.fromString(POST("/dataModels/$dataModelId/dataTypes", [label: 'Test data type', domainType: 'PrimitiveType']).id)

        when:
        exportModel = GET("/dataModels/$dataModelId/export/uk.ac.ox.softeng.mauro.plugin.exporter.json/JsonDataModelExporterPlugin/4.0.0", ExportModel)
        then:
        exportModel.dataModel.label == 'Test data model'
        exportModel.dataModel.dataTypes.label == ['Test data type']
        exportModel.dataModel.dataClasses.label.sort() == ['TEST-1', 'TEST-2'] // TODO add ordering to DataClasses, Elements, etc.
    }

    void 'import dataModel and verify'() {
        given:
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
