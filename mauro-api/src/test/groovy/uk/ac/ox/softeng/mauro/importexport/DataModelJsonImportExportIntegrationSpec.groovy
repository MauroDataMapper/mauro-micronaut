package uk.ac.ox.softeng.mauro.importexport

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
class DataModelJsonImportExportIntegrationSpec extends CommonDataSpec {
    @Inject
    ObjectMapper objectMapper

    @Inject
    EmbeddedApplication<?> application

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

    void setupSpec(){

        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        DataModel dataModelResponse = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model'], DataModel)
        dataModelId = dataModelResponse.id

        Metadata metadataResponse = (Metadata) POST("$DATAMODELS_PATH/$dataModelId$METADATA_PATH", metadataPayload(), Metadata)
        metadataId = metadataResponse.id

        SummaryMetadata summaryMetadataResponse = (SummaryMetadata) POST("$DATAMODELS_PATH/$dataModelId$SUMMARY_METADATA_PATH",
               summaryMetadataPayload(), SummaryMetadata)
        summaryMetadataId = summaryMetadataResponse.id

        SummaryMetadataReport reportResponse = (SummaryMetadataReport) POST("$DATAMODELS_PATH/$dataModelId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)
        reportId = reportResponse.id

        Annotation annotationResponse = (Annotation) POST("$DATAMODELS_PATH/$dataModelId/$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        annotationId = annotationResponse.id

        Annotation childResp = (Annotation) POST("$DATAMODELS_PATH/$dataModelId$ANNOTATION_PATH/$annotationId$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)
        childAnnotationId = childResp.id

        dataType = (DataType) POST("$DATAMODELS_PATH/$dataModelId/dataTypes", [label: 'string', description: 'character string of variable length', domainType: 'PrimitiveType'], DataType)
    }

    void 'test get export data model - should export model'() {
        when:
        String json = GET("$DATAMODELS_PATH/$dataModelId$EXPORT_PATH", String)

        then:
        json

        Map parsedJson = jsonSlurper.parseText(json) as Map
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
        String exportJson = GET("$DATAMODELS_PATH/$dataModelId$EXPORT_PATH", String)
        exportJson

        and:
        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, exportJson.bytes)
                .build()
        when:
        ListResponse<DataModel> response = (ListResponse<DataModel>) POST("$DATAMODELS_PATH$IMPORT_PATH/mauro/micronaut/SNAPSHOT", importRequest)

        then:
        response
        UUID importedDataModelId = UUID.fromString(response.items.id.first() as String)

        when:
        DataModel importedDataModel = (DataModel) GET("$DATAMODELS_PATH/$importedDataModelId", DataModel)

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
        ListResponse<DataType> importedDataTypes = (ListResponse<DataType>)  GET("$DATAMODELS_PATH/$importedDataModelId$DATATYPES_PATH", ListResponse<DataType>)
        then:
        importedDataTypes
        importedDataTypes.count == 1
        importedDataTypes.items.domainType.first() == dataType.domainType
    }

}
