package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

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

    @Shared
    UUID summaryMetadataId

    @Shared
    UUID summaryMetadataReportId

    @Shared
    UUID dataElementId

    @Override
    def dataElementPayload(String label, DataType dataType) {
        return super.dataElementPayload(label, dataType)
    }
    JsonSlurper jsonSlurper = new JsonSlurper()

    void 'create dataModel and export'() {
        given:
        folderId = ((Folder) POST("$FOLDERS_PATH", [label: 'Test folder'], Folder)).id
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model'], DataModel)).id
        UUID dataClass1Id = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class'], DataClass)).id
        UUID dataClass2Id = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-2', definition: 'second data class'], DataClass)).id
        UUID dataTypeId = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'Test data type', domainType: 'PrimitiveType'],DataType)).id
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClass1Id$DATA_ELEMENTS_PATH",
                [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId]], DataElement)).id
        summaryMetadataId = ((SummaryMetadata) (POST("$DATA_ELEMENTS_PATH/$dataElementId$SUMMARY_METADATA_PATH", summaryMetadataPayload(), SummaryMetadata))).id
        summaryMetadataReportId = ((SummaryMetadataReport) POST("$DATA_ELEMENTS_PATH/$dataElementId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH", summaryMetadataReport(), SummaryMetadataReport)).id

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
        def dataClass = dataClasses.items.find { it.path.contains('dm:Test data model$main|dc:TEST-1')}

        UUID importedDataClassId = UUID.fromString(dataClass.id)

        when:
        def dataTypes = GET("/dataModels/$importedDataModelId/dataTypes")

        then:
        dataTypes.items.path == ['dm:Test data model$main|dt:Test data type']

        when:
        ListResponse<DataElement> copyDataElements  = (ListResponse<DataElement>) GET("$DATAMODELS_PATH/$importedDataModelId$DATACLASSES_PATH/$importedDataClassId$DATA_ELEMENTS_PATH")

        then:
        copyDataElements

        def copyDataElementId = copyDataElements.items.first().id
        copyDataElementId != dataElementId


        when:
        ListResponse<SummaryMetadata> copiedSummaryMetadata = (ListResponse<SummaryMetadata>) GET("$DATA_ELEMENTS_PATH/$copyDataElementId$SUMMARY_METADATA_PATH")
        then:
        copiedSummaryMetadata
        copiedSummaryMetadata.items.size() == 1
        def copiedSummaryMetadataId = copiedSummaryMetadata.items.first().id
        copiedSummaryMetadataId != summaryMetadataId

        when:
        ListResponse<SummaryMetadataReport> copiedSummaryMetadataReport = (ListResponse<SummaryMetadataReport>) GET("$DATA_ELEMENTS_PATH/$copyDataElementId$SUMMARY_METADATA_PATH/$copiedSummaryMetadataId$SUMMARY_METADATA_REPORT_PATH")
        then:
        copiedSummaryMetadataReport
        copiedSummaryMetadataReport.items.size() == 1
        copiedSummaryMetadataReport.items.first().id != summaryMetadataReportId
    }
}
