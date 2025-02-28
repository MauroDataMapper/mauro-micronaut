package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.web.PaginationParams

@ContainerizedTest
@Singleton
class DataModelJsonImportExportSpec extends CommonDataSpec {

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
    DataElement dataElementPayload(String label, DataType dataType) {
        return super.dataElementPayload(label, dataType)
    }
    JsonSlurper jsonSlurper = new JsonSlurper()

    void 'create dataModel and export'() {
        given:
        folderId = folderApi.create( new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, new DataModel(label: 'Test data model')).id
        UUID dataClass1Id = dataClassApi.create(
            dataModelId,
            new DataClass(label: 'TEST-1', description: 'first data class')).id
        UUID dataClass2Id = dataClassApi.create(
            dataModelId,
            new DataClass(label: 'TEST-2', description: 'second data class')).id
        dataClassApi.createExtension(dataModelId, dataClass2Id, dataModelId, dataClass1Id)
        UUID dataTypeId = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'Test data type', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id
        dataElementId = dataElementApi.create(
            dataModelId,
            dataClass1Id,
            new DataElement(
                label: 'First data element',
                description: 'The first data element',
                dataType: new DataType(id: dataTypeId))).id
        summaryMetadataId = summaryMetadataApi.create(
            'dataElements',
            dataElementId,
            summaryMetadataPayload()).id
        summaryMetadataReportId = summaryMetadataReportApi.create(
            'dataElements',
            dataElementId,
            summaryMetadataId,
            summaryMetadataReport()).id

        when:

        String json = new String(dataModelApi.exportModel(
            dataModelId,
            'uk.ac.ox.softeng.mauro.plugin.exporter.json',
            'JsonDataModelExporterPlugin',
            '4.0.0').body())

        then:
        json
        Map parsedJson = jsonSlurper.parseText(json) as Map
        parsedJson.exportMetadata
        parsedJson.dataModel.id == dataModelId.toString()
        parsedJson.dataModel.label == 'Test data model'
        parsedJson.dataModel.dataClasses.size() == 2
        parsedJson.dataModel.dataClasses.find {it.label == 'TEST-2' && it.extendsDataClasses.size() == 1 && it.extendsDataClasses.first().label == 'TEST-1'}
        parsedJson.dataModel.dataClasses.dataElements.flatten().id[0] == dataElementId.toString()
        parsedJson.dataModel.dataClasses.dataElements.summaryMetadata.flatten().id[0] == summaryMetadataId.toString()
        parsedJson.dataModel.dataClasses.dataElements.summaryMetadata.summaryMetadataReports.flatten().id[0] == summaryMetadataReportId.toString()
    }

    void 'import dataModel and verify'() {
        given:
        byte[] responseBytes = dataModelApi.exportModel(
            dataModelId,
            'uk.ac.ox.softeng.mauro.plugin.exporter.json',
            'JsonDataModelExporterPlugin',
            '4.0.0').body()

        exportModel = objectMapper.readValue(responseBytes, ExportModel)


        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()
        ListResponse<DataModel> response =
            dataModelApi.importModel(
                importRequest,
                'uk.ac.ox.softeng.mauro.plugin.importer.json',
                'JsonDataModelImporterPlugin',
                '4.0.0')

        when:
        UUID importedDataModelId = response.items.first().id
        DataModel dataModel = dataModelApi.show(importedDataModelId)

        then:
        dataModel.path.toString() == 'fo:Test folder|dm:Test data model$main'

        when:
        ListResponse<DataClass> dataClasses = dataClassApi.list(importedDataModelId, new PaginationParams())

        then:
        DataClass dataClassResponse = dataClassApi.show(importedDataModelId, dataClasses.items.find { it.label == 'TEST-2'}.id)
        dataClassResponse.extendsDataClasses.size() == 1
        dataClassResponse.extendsDataClasses.first().label == 'TEST-1'
        dataClasses.items.path.collect { it.toString()}.sort() == ['fo:Test folder|dm:Test data model$main|dc:TEST-1', 'fo:Test folder|dm:Test data model$main|dc:TEST-2']
        DataClass dataClass = dataClasses.items.find { it.path.toString().contains('fo:Test folder|dm:Test data model$main|dc:TEST-1')}
        UUID importedDataClassId = dataClass.id

        when:
        ListResponse<DataType> dataTypes = dataTypeApi.list(importedDataModelId)

        then:
        dataTypes.items.path.collect { it.toString()} == ['fo:Test folder|dm:Test data model$main|dt:Test data type']

        when:
        ListResponse<DataElement> copyDataElements  = dataElementApi.list(
            importedDataModelId,
            importedDataClassId,
        new PaginationParams())

        then:
        copyDataElements

        def copyDataElementId = copyDataElements.items.first().id
        copyDataElementId != dataElementId


        when:
        ListResponse<SummaryMetadata> copiedSummaryMetadata =
            summaryMetadataApi.list("dataElement", copyDataElementId, new PaginationParams())
        then:
        copiedSummaryMetadata
        copiedSummaryMetadata.items.size() == 1
        def copiedSummaryMetadataId = copiedSummaryMetadata.items.first().id
        copiedSummaryMetadataId != summaryMetadataId

        when:
        ListResponse<SummaryMetadataReport> copiedSummaryMetadataReport =
            summaryMetadataReportApi.list("dataElement", copyDataElementId, copiedSummaryMetadataId)
        then:
        copiedSummaryMetadataReport
        copiedSummaryMetadataReport.items.size() == 1
        copiedSummaryMetadataReport.items.first().id != summaryMetadataReportId
    }
}
