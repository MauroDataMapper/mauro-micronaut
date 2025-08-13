package org.maurodata.dataflow

import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
    "classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_ALL)
class DataFlowImportExportIntegrationSpec extends CommonDataSpec {

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


    void setupSpec() {
        folderId = folderApi.create(folder()).id
        source = dataModelApi.create(folderId, dataModelPayload('source label'))
        target = dataModelApi.create(folderId, dataModelPayload('target label'))
        dataType1 = dataTypeApi.create(source.id, dataTypesPayload('dataType1 label', DataType.DataTypeKind.PRIMITIVE_TYPE))
        dataType2 = dataTypeApi.create(target.id, dataTypesPayload())
        dataClassId1 = dataClassApi.create(source.id, dataClassPayload('dataclass label 1')).id
        dataClassId2 = dataClassApi.create(target.id, dataClassPayload('dataclass label 2')).id
        dataElementId1 = dataElementApi.create(target.id, dataClassId1, dataElementPayload('dataElement1 label', dataType1)).id
        dataElementId2 = dataElementApi.create(target.id, dataClassId2, dataElementPayload('label2', dataType2)).id

        dataFlow = dataFlowApi.create(target.id, new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: source))

        dataClassComponentId = dataClassComponentApi.create(source.id, dataFlow.id,
                                                            new DataClassComponent(
                                                                label: 'data class component test label')).id
        dataClassComponentApi.updateSource(source.id, dataFlow.id, dataClassComponentId, dataClassId1)
        dataClassComponentApi.updateTarget(source.id, dataFlow.id, dataClassComponentId, dataClassId2)
        dataElementComponentId =
            dataElementComponentApi.create(source.id, dataFlow.id, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(source.id, dataFlow.id, dataClassComponentId, dataElementComponentId, dataElementId1)
        dataElementComponentApi.updateTarget(source.id, dataFlow.id, dataClassComponentId, dataElementComponentId, dataElementId2)
    }


    void 'shouldCreateExportModel'() {
        Metadata metadata = metadataApi.create(DataFlow.simpleName, dataFlow.id, metadataPayload())

        SummaryMetadata summaryMetadata = summaryMetadataApi.create(DataFlow.simpleName, dataFlow.id, summaryMetadataPayload())

        SummaryMetadataReport summaryMetadataReport = summaryMetadataReportApi.create(DataFlow.simpleName, dataFlow.id, summaryMetadata.id, summaryMetadataReport())

        Annotation annotation = annotationApi.create(DataFlow.simpleName, dataFlow.id, annotationPayload())
        Annotation childAnnotation = annotationApi.create(DataFlow.simpleName, dataFlow.id, annotation.id, annotationPayload('childLabel', 'child-description'))


        when:
        HttpResponse<byte[]> exportResponse =
            dataFlowApi.exportModel(target.id, dataFlow.id, 'org.maurodata.plugin.exporter.json', 'JsonDataFlowExporterPlugin', '4.0.0')
        then:
        exportResponse

        exportResponse.body()
        Map parsedJson = jsonSlurper.parseText(new String(exportResponse.body())) as Map
        parsedJson.exportMetadata
        parsedJson.dataFlow
        parsedJson.dataFlow.source
        parsedJson.dataFlow.source.get('id') == source.id.toString()
        parsedJson.dataFlow.target
        parsedJson.dataFlow.target.get('id') == target.id.toString()

        parsedJson.dataFlow.metadata.size() == 1
        parsedJson.dataFlow.metadata[0].id != metadata.id
        parsedJson.dataFlow.summaryMetadata.size() == 1
        parsedJson.dataFlow.summaryMetadata[0].id != summaryMetadata.id
        parsedJson.dataFlow.summaryMetadata[0].summaryMetadataReports.size() == 1
        parsedJson.dataFlow.summaryMetadata[0].summaryMetadataReports[0].id != summaryMetadataReport.id

        parsedJson.dataFlow.annotations.size() == 1
        parsedJson.dataFlow.annotations[0].id != annotation.id
        parsedJson.dataFlow.annotations[0].childAnnotations.size() == 1
        parsedJson.dataFlow.annotations[0].childAnnotations[0].id != childAnnotation.id


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


}
