package org.maurodata.importexport

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.folder.Folder
import org.maurodata.export.ExportModel
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

import java.time.Instant

@ContainerizedTest
@Singleton
class DataModelJsonImportExportIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId
    @Shared
    DataModel source
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
    @Shared
    DataType dataType2
    @Shared
    UUID dataElementId1
    @Shared
    UUID dataElementId2
    @Shared
    UUID dataClassId1
    @Shared
    UUID dataClassId2

    @Shared
    DataFlow dataFlow
    @Shared
    UUID dataClassComponentId
    @Shared
    UUID dataElementComponentId


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

        dataClassId1 = dataClassApi.create(dataModelId, dataClassPayload('dataClass label')).id

        dataType = dataTypeApi.create(dataModelId,
                                      new DataType(label: 'string', description: 'character string of variable length', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))

        dataElementId1 = dataElementApi.create(dataModelId, dataClassId1, dataElementPayload('data element label', dataType)).id

        source = dataModelApi.create(folderId, dataModelPayload('source label'))

        dataFlow = dataFlowApi.create(dataModelId, new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: source))

        dataClassComponentId = dataClassComponentApi.create(dataModelId, dataFlow.id,
                                                            new DataClassComponent(
                                                                label: 'data class component test label')).id
        dataClassComponentApi.updateTarget(dataModelId, dataFlow.id, dataClassComponentId, dataClassId1)

        dataElementComponentId =
            dataElementComponentApi.create(dataModelId, dataFlow.id, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateTarget(dataModelId, dataFlow.id, dataClassComponentId, dataElementComponentId, dataElementId1)

    }

    void 'test get export data model - should export model'() {
        when:
        HttpResponse<byte[]> response = dataModelApi.exportModel(dataModelId, 'org.maurodata.plugin.exporter.json', 'JsonDataModelExporterPlugin', '4.0.0')

        then:
        response.body()

        Map parsedJson = jsonSlurper.parseText(new String(response.body())) as Map
        parsedJson.exportMetadata

        parsedJson.dataModel
        parsedJson.dataModel.path
        parsedJson.dataModel.id == dataModelId.toString()
        parsedJson.dataModel.metadata.id == List.of(metadataId.toString())
        parsedJson.dataModel.summaryMetadata.id == List.of(summaryMetadataId.toString())
        parsedJson.dataModel.summaryMetadata.summaryMetadataReports[0]
        parsedJson.dataModel.summaryMetadata.summaryMetadataReports[0].id == List.of(reportId.toString())
        parsedJson.dataModel.annotations.id == List.of(annotationId.toString())
        parsedJson.dataModel.annotations.childAnnotations[0].id == List.of(childAnnotationId.toString())
        parsedJson.dataModel.dataTypes.id == List.of(dataType.id.toString())
        parsedJson.dataModel.dataTypes.domainType == List.of(dataType.domainType)


        parsedJson.dataModel.targetDataFlows
        parsedJson.dataModel.targetDataFlows.size() == 1
        parsedJson.dataModel.targetDataFlows[0].path
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents.size() == 1
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].path

        !parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].sourceDataClasses

        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].targetDataClasses
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].targetDataClasses.size() == 1
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].targetDataClasses[0].id == dataClassId1.toString()
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].targetDataClasses[0].path
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents.size() == 1
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].id == dataElementComponentId.toString()
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].path
        !parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].sourceDataElements
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements.size() == 1
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements[0].id == dataElementId1.toString()
        parsedJson.dataModel.targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements[0].path
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
        importedDataModelId != dataModelId


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
        List<Annotation> children = importedDataModel.annotations[0].childAnnotations
        children.size() == 1
        children.first().parentAnnotationId == parentId

        when:
        ListResponse<DataType> importedDataTypes = dataTypeApi.list(importedDataModelId)
        then:
        importedDataTypes
        importedDataTypes.count == 1
        importedDataTypes.items.domainType.first() == dataType.domainType

        //check imported dataModel includes dataFlows
        when:
        ListResponse<DataFlow> dataFlowResponse = dataFlowApi.list(importedDataModelId, Type.TARGET)

        then:
        dataFlowResponse
        dataFlowResponse.items.size() == 1
        dataFlowResponse.items[0].target.id == importedDataModelId

        when:
        DataFlow importedDataFlow = dataFlowApi.show(importedDataModelId, dataFlowResponse.items[0].id)
        then:
        importedDataFlow
        importedDataFlow.id != dataFlow.id

        when:
        ListResponse<DataClassComponent> importedDataClassComponents = dataClassComponentApi.list(importedDataModel.id, importedDataFlow.id)
        then:
        importedDataClassComponents
        importedDataClassComponents.items.size() == 1

        when:
        DataClassComponent importedDataClassComponent = dataClassComponentApi.show(importedDataModel.id, importedDataFlow.id, importedDataClassComponents.items[0].id)
        then:
        importedDataClassComponent
        importedDataClassComponent.id != dataClassComponentId

        when:
        ListResponse<DataElementComponent> importedDataElementComponents = dataElementComponentApi.list(importedDataModel.id, importedDataFlow.id, importedDataClassComponent.id)
        then:
        importedDataElementComponents
        importedDataElementComponents.items.size() == 1
        importedDataElementComponents.items[0].id != dataElementComponentId
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
