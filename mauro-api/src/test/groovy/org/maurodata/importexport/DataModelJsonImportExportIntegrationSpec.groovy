package org.maurodata.importexport

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.export.ExportModel
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import java.time.Instant

@ContainerizedTest
@Singleton
class DataModelJsonImportExportIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID metadataId

    @Shared
    UUID summaryMetadataId

    @Shared
    UUID reportId

    @Shared
    UUID annotationId

    @Shared
    UUID childAnnotationId

    @Shared
    DataType dataType

    JsonSlurper jsonSlurper = new JsonSlurper()

    void setup(){

        Folder response = folderApi.create(folder())
        folderId = response.id

        DataModel dataModelResponse = dataModelApi.create(folderId, new DataModel(label: 'Test data model'))
        dataModelId = dataModelResponse.id

        Metadata metadataResponse = metadataApi.create("dataModel", dataModelId, metadataPayload())
        metadataId = metadataResponse.id

        SummaryMetadata summaryMetadataResponse = summaryMetadataApi.create("dataModel", dataModelId, summaryMetadataPayload())
        summaryMetadataId = summaryMetadataResponse.id

        SummaryMetadataReport reportResponse = summaryMetadataReportApi.create("dataModel", dataModelId, summaryMetadataId, summaryMetadataReport())
        reportId = reportResponse.id

        Annotation annotationResponse = annotationApi.create("dataModel", dataModelId, annotationPayload())
        annotationId = annotationResponse.id

        Annotation childResp = annotationApi.create("dataModel", dataModelId, annotationId, annotationPayload('child label', 'child description'))
        childAnnotationId = childResp.id

        dataType = dataTypeApi.create(dataModelId,
            new DataType(label: 'string', description: 'character string of variable length', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
    }

    void 'test get export data model - should export model'() {
        when:
        HttpResponse<byte[]> response = dataModelApi.exportModel(dataModelId, 'org.maurodata.plugin.exporter.json', 'JsonDataModelExporterPlugin', '4.0.0')

        then:
        response.body()

        Map parsedJson = jsonSlurper.parseText(new String(response.body())) as Map
        parsedJson.exportMetadata

        parsedJson.dataModel
        parsedJson.dataModel.id == dataModelId.toString()
        parsedJson.dataModel.metadata.id == List.of(metadataId.toString())
        parsedJson.dataModel.summaryMetadata.id == List.of(summaryMetadataId.toString())
        parsedJson.dataModel.summaryMetadata.summaryMetadataReports[0]
        parsedJson.dataModel.summaryMetadata.summaryMetadataReports[0].id == List.of(reportId.toString())
        parsedJson.dataModel.annotations.id == List.of(annotationId.toString())
        parsedJson.dataModel.annotations.childAnnotations[0].id == List.of(childAnnotationId.toString())
        parsedJson.dataModel.dataTypes.id == List.of(dataType.id.toString())
        parsedJson.dataModel.dataTypes.domainType == List.of(dataType.domainType)
    }


    void 'test consume export data model  - should import'() {
        given:
        HttpResponse<byte[]> response = dataModelApi.exportModel(dataModelId, 'org.maurodata.plugin.exporter.json', 'JsonDataModelExporterPlugin', '4.0.0')
        response.body()

        and:
        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, response.body())
                .build()
        when:
        ListResponse<DataModel> dataModelResponse = dataModelApi.importModel(importRequest, 'org.maurodata.plugin.importer.json', 'JsonDataModelImporterPlugin', '4.0.0')

        then:
        dataModelResponse
        UUID importedDataModelId = dataModelResponse.items.first().id

        when:
        DataModel importedDataModel = dataModelApi.show(importedDataModelId)

        then:
        importedDataModel
        importedDataModel.domainType == 'DataModel'
        importedDataModel.metadata.size() == 1
        importedDataModel.summaryMetadata.size() == 1
        UUID summaryMetadataId = importedDataModel.summaryMetadata[0].id
        importedDataModel.summaryMetadata.summaryMetadataReports.size() == 1
        SummaryMetadataReport report = importedDataModel.summaryMetadata[0].summaryMetadataReports[0]
        report.summaryMetadataId == summaryMetadataId

        importedDataModel.annotations.size() == 1
        UUID parentId = importedDataModel.annotations[0].id
        List<Annotation> children  = importedDataModel.annotations[0].childAnnotations
        children.size() == 1
        children.first().parentAnnotationId == parentId

        when:
        ListResponse<DataType> importedDataTypes = dataTypeApi.list(importedDataModelId)
        then:
        importedDataTypes
        importedDataTypes.count == 1
        importedDataTypes.items.domainType.first() == dataType.domainType
    }

    // TODO: Is this used, or could it be?
    UUID importModelViaApi(UUID folderId, DataModel dataModel1, ObjectMapper objectMapper1) {

        ExportModel exportModel = ExportModel.build {
            dataModel dataModel1
            exportMetadata {
                namespace 'Test namespace'
                name 'Test name'
                version 'Test version'
                exportDate Instant.now()
                exportedBy "Anonymous User"
            }
        }
        byte[] payload = objectMapper1.writeValueAsBytes(exportModel)


        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, payload)
                .build()

        ListResponse<DataModel> response = dataModelApi.importModel(importRequest, 'org.maurodata.plugin.importer.json', 'JsonDataModelImporterPlugin', '4.0.0')
        response.items[0].id

    }

}
