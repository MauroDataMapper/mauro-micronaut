package org.maurodata.importexport


import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.apache.http.HttpStatus
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.export.ExportModel
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
    "classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataFlowImportExportIntegrationSpec extends CommonDataSpec {

    static final String EXPORTER_NAMESPACE = 'org.maurodata.plugin.exporter.json'
    static final String EXPORTER_NAME = 'JsonDataFlowExporterPlugin'
    static final String EXPORTER_VERSION = '4.0.0'
    static final String IMPORTER_NAMESPACE = 'org.maurodata.plugin.importer.json'
    static final String IMPORTER_NAME = 'JsonDataFlowImporterPlugin'
    static final String IMPORTER_VERSION = '4.0.0'

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Shared
    UUID folderId
    @Shared
    DataModel source
    @Shared
    DataModel target
    @Shared
    UUID dataClassId1
    @Shared
    UUID dataClassId2
    @Shared
    DataType dataType1
    @Shared
    DataType dataType2
    @Shared
    UUID dataElementId1
    @Shared
    UUID dataElementId2

    @Shared
    UUID dataClassComponentId
    @Shared
    UUID dataElementComponentId
    @Shared
    DataFlow dataFlow

    @Shared
    UUID annotationId
    @Shared
    UUID childAnnotationId
    @Shared
    UUID metadataId
    @Shared
    UUID summaryMetadataId
    @Shared
    UUID summaryMetadataReportId
    @Shared
    DataModel sourceBranch
    @Shared
    DataModel targetBranch

    void setup() {
        folderId = folderApi.create(folder()).id
        source = dataModelApi.create(folderId, dataModelPayload('source label'))
        dataType1 = dataTypeApi.create(source.id, dataTypesPayload('dataType1 label', DataType.DataTypeKind.PRIMITIVE_TYPE))
        dataClassId1 = dataClassApi.create(source.id, dataClassPayload('dataclass label 1')).id
        dataElementId1 = dataElementApi.create(source.id, dataClassId1, dataElementPayload('dataElement1 label', dataType1)).id
        sourceBranch =
            dataModelApi.createNewBranchModelVersion(
                source.id,
                new CreateNewVersionData(branchName: 'new branch name' ))

        target = dataModelApi.create(folderId, dataModelPayload('target label'))
        dataType2 = dataTypeApi.create(target.id, dataTypesPayload())
        dataClassId2 = dataClassApi.create(target.id, dataClassPayload('dataclass label 2')).id
        dataElementId2 = dataElementApi.create(target.id, dataClassId2, dataElementPayload('label2', dataType2)).id
        targetBranch =
            dataModelApi.createNewBranchModelVersion(
                target.id,
                new CreateNewVersionData(branchName: 'targetBranch' ))

        dataFlow = dataFlowApi.create(target.id, new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: source))

        dataClassComponentId = dataClassComponentApi.create(target.id, dataFlow.id,
                                                            new DataClassComponent(
                                                                label: 'data class component test label')).id
        dataClassComponentApi.updateSource(target.id, dataFlow.id, dataClassComponentId, dataClassId1)
        dataClassComponentApi.updateTarget(target.id, dataFlow.id, dataClassComponentId, dataClassId2)
        dataElementComponentId =
            dataElementComponentApi.create(target.id, dataFlow.id, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(source.id, dataFlow.id, dataClassComponentId, dataElementComponentId, dataElementId1)
        dataElementComponentApi.updateTarget(target.id, dataFlow.id, dataClassComponentId, dataElementComponentId, dataElementId2)

        metadataId = metadataApi.create(DataFlow.simpleName, dataFlow.id, metadataPayload()).id
        summaryMetadataId = summaryMetadataApi.create(DataFlow.simpleName, dataFlow.id, summaryMetadataPayload()).id
        summaryMetadataReportId = summaryMetadataReportApi.create(DataFlow.simpleName, dataFlow.id, summaryMetadataId, summaryMetadataReport()).id
        annotationId = annotationApi.create(DataFlow.simpleName, dataFlow.id, annotationPayload()).id
        childAnnotationId = annotationApi.create(DataFlow.simpleName, dataFlow.id, annotationId, annotationPayload('childLabel', 'child-description')).id
    }


    void 'shouldCreateExportModel'() {
        when:
        HttpResponse<byte[]> exportFile =
            dataFlowApi.exportModel(target.id, dataFlow.id, EXPORTER_NAMESPACE, EXPORTER_NAME, EXPORTER_VERSION)
        then:
        exportFile

        Map parsedJson = jsonSlurper.parseText(new String(exportFile.body())) as Map
        parsedJson.exportMetadata
        parsedJson.dataFlow
        parsedJson.dataFlow.source
        parsedJson.dataFlow.source.get('id') == source.id.toString()
        parsedJson.dataFlow.target
        parsedJson.dataFlow.target.get('id') == target.id.toString()

        parsedJson.dataFlow.metadata.size() == 1
        parsedJson.dataFlow.metadata[0].id != metadataId
        parsedJson.dataFlow.summaryMetadata.size() == 1
        parsedJson.dataFlow.summaryMetadata[0].id != summaryMetadataId
        parsedJson.dataFlow.summaryMetadata[0].summaryMetadataReports.size() == 1
        parsedJson.dataFlow.summaryMetadata[0].summaryMetadataReports[0].id != summaryMetadataReportId

        parsedJson.dataFlow.annotations.size() == 1
        parsedJson.dataFlow.annotations[0].id != annotationId
        parsedJson.dataFlow.annotations[0].childAnnotations.size() == 1
        parsedJson.dataFlow.annotations[0].childAnnotations[0].id != childAnnotationId


        parsedJson.dataFlow.dataClassComponents.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].sourceDataClasses.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].targetDataClasses.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].dataElementComponents.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].sourceDataClasses[0].id != dataClassId1
        parsedJson.dataFlow.dataClassComponents[0].targetDataClasses[0].id != dataClassId2

        parsedJson.dataFlow.dataClassComponents[0].dataElementComponents[0].sourceDataElements.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].dataElementComponents[0].targetDataElements.size() == 1
        parsedJson.dataFlow.dataClassComponents[0].dataElementComponents[0].sourceDataElements[0].id != dataElementId1
        parsedJson.dataFlow.dataClassComponents[0].dataElementComponents[0].targetDataElements[0].id != dataElementId2

    }

    void 'shouldImportDataFlow'() {
        given:
        byte[] responseBytes =
            dataFlowApi.exportModel(target.id, dataFlow.id, EXPORTER_NAMESPACE, EXPORTER_NAME, EXPORTER_VERSION).body()

        ExportModel exportModel = objectMapper.readValue(responseBytes, ExportModel)

        and:
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('sourceDataModelId', sourceBranch.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()

        when:
        ListResponse<DataFlow> dataFlowResponse = dataFlowApi.importModel(targetBranch.id, importRequest, IMPORTER_NAMESPACE, IMPORTER_NAME, IMPORTER_VERSION)

        then:
        dataFlowResponse
        dataFlowResponse.items.size() == 1
        DataFlow response = dataFlowResponse.items[0]
        response.source
        response.target
        response.source.id == sourceBranch.id
        response.target.id == targetBranch.id
        response.annotations.size() == 1
        response.annotations[0].id != annotationId
        response.annotations[0].childAnnotations.size() == 1
        response.annotations[0].childAnnotations[0].id != childAnnotationId
        response.metadata.size() == 1
        response.metadata[0].id != metadataId
        response.summaryMetadata.size() == 1
        response.summaryMetadata[0].id != summaryMetadataId
        response.summaryMetadata[0].summaryMetadataReports.size() == 1
        response.summaryMetadata[0].summaryMetadataReports[0].id != summaryMetadataReportId

        //proof new source/target rows  created -DataClassComponent->dataClass, dataElementComponent->dataElement
        and:
        HttpResponse httpResponse = dataClassComponentApi.deleteSource(sourceBranch.id, dataFlowResponse.items[0].id, dataClassComponentId, dataClassId1)
        then:
        httpResponse.status().code == HttpStatus.SC_NO_CONTENT

        when:
        httpResponse = dataElementComponentApi.deleteTarget(targetBranch.id, dataFlowResponse.items[0].id, dataClassComponentId, dataElementComponentId, dataElementId2)
        then:
        httpResponse.status().code == HttpStatus.SC_NO_CONTENT
    }

    void 'import dataflow - target datamodel does not contain same model structure or model items as exported datamodel target -should throw exception'() {
        given:
        byte[] responseBytes =
            dataFlowApi.exportModel(target.id, dataFlow.id, EXPORTER_NAMESPACE, EXPORTER_NAME, EXPORTER_VERSION).body()

        ExportModel exportModel = objectMapper.readValue(responseBytes, ExportModel)

        and:
        DataModel importTarget = dataModelApi.create(folderId, dataModelPayload('import target  label'))

        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('sourceDataModelId', sourceBranch.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()

        when:
        dataFlowApi.importModel(importTarget.id, importRequest, IMPORTER_NAMESPACE, IMPORTER_NAME, IMPORTER_VERSION)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

    }
    void 'import dataflow - source datamodel does not contain same model structure or model items as exported datamodel source -should throw exception'() {
        given:
        byte[] responseBytes =
            dataFlowApi.exportModel(target.id, dataFlow.id, EXPORTER_NAMESPACE, EXPORTER_NAME, EXPORTER_VERSION).body()

        ExportModel exportModel = objectMapper.readValue(responseBytes, ExportModel)

        and:
        DataModel importSource = dataModelApi.create(folderId, dataModelPayload('import target  label'))

        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('sourceDataModelId', importSource.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()

        when:
        dataFlowApi.importModel(targetBranch.id, importRequest, IMPORTER_NAMESPACE, IMPORTER_NAME, IMPORTER_VERSION)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

    }

    void 'importing dataflow -target model is finalised  -should throw exception '() {
        given:
        DataModel anotherModel = dataModelApi.create(folderId, dataModelPayload('another model label'))
        dataModelApi.finalise(anotherModel.id, new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'random version tag'))

        byte[] responseBytes =
            dataFlowApi.exportModel(target.id, dataFlow.id, EXPORTER_NAMESPACE, EXPORTER_NAME, EXPORTER_VERSION).body()

        ExportModel exportModel = objectMapper.readValue(responseBytes, ExportModel)

        and:
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('sourceDataModelId', sourceBranch.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()


        when:
         dataFlowApi.importModel(anotherModel.id, importRequest, IMPORTER_NAMESPACE, IMPORTER_NAME, IMPORTER_VERSION)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

    }

}
