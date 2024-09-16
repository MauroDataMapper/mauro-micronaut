package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
class DataModelWithFacetsIntegrationSpec extends CommonDataSpec {
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

    @Shared
    ReferenceFile referenceFile



    void setupSpec(){
        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        DataModel dataModelResponse = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model', description: 'test description', author: 'test author'], DataModel)
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
        String fileContent = "a  very long string the quick brown fox jumped over the dog"

        referenceFile = (ReferenceFile) POST ("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH",
                ["fileName": "test file name",
                    "fileSize": fileContent.size(),
                    "fileContents": fileContent.bytes,
                    "fileType": "text/plain"], ReferenceFile)
    }

    void 'test get data model with facets - should return all nested facets'() {
        when:
        DataModel retrieved = (DataModel) GET("$DATAMODELS_PATH/$dataModelId", DataModel)
        then:
        retrieved
        retrieved.metadata
        retrieved.metadata.size() == 1
        retrieved.metadata.first().id == metadataId
        retrieved.summaryMetadata
        retrieved.summaryMetadata.size() == 1
        retrieved.summaryMetadata.first().id == summaryMetadataId
        retrieved.summaryMetadata.first().summaryMetadataReports
        retrieved.summaryMetadata.first().summaryMetadataReports.first().id == reportId
        retrieved.summaryMetadata.first().summaryMetadataReports.first().id == reportId
        retrieved.annotations
        retrieved.annotations.size() == 1
        retrieved.annotations.first().id == annotationId
        retrieved.annotations.first().childAnnotations.size() == 1
        retrieved.annotations.first().childAnnotations.first().parentAnnotationId == annotationId

        retrieved.referenceFiles
        retrieved.referenceFiles.size() == 1
        retrieved.referenceFiles[0].id == referenceFile.id
        !retrieved.referenceFiles[0].fileContents
        retrieved.referenceFiles[0].fileSize == referenceFile.fileSize
    }


    void 'test delete data model with facets - should delete model and all related facets'() {
        given:
        DataModel retrieved = (DataModel) GET("$DATAMODELS_PATH/$dataModelId", DataModel)

        and:
        retrieved
        retrieved.metadata
        retrieved.metadata.size() == 1
        retrieved.metadata.first().id == metadataId
        retrieved.summaryMetadata
        retrieved.summaryMetadata.size() == 1
        retrieved.summaryMetadata.first().id == summaryMetadataId
        retrieved.summaryMetadata.first().summaryMetadataReports
        retrieved.summaryMetadata.first().summaryMetadataReports.first().id == reportId
        retrieved.summaryMetadata.first().summaryMetadataReports.first().id == reportId
        retrieved.annotations
        retrieved.annotations.size() == 1
        retrieved.annotations.first().id == annotationId
        retrieved.annotations.first().childAnnotations.size() == 1
        retrieved.annotations.first().childAnnotations.first().parentAnnotationId == annotationId

        retrieved.referenceFiles
        retrieved.referenceFiles.size() == 1
        retrieved.referenceFiles.first().id == referenceFile.id

        when:
        HttpStatus httpStatus = DELETE("$DATAMODELS_PATH/$dataModelId", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        when:
        GET("$DATAMODELS_PATH/$dataModelId", DataModel)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        GET("$DATAMODELS_PATH/$dataModelId$METADATA_PATH/$metadataId", Metadata)

        then: 'the show endpoint shows the update'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        GET("$DATAMODELS_PATH/$dataModelId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH/$reportId", SummaryMetadataReport)

        then: 'the show endpoint shows the update'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        GET("$DATAMODELS_PATH/$dataModelId$ANNOTATION_PATH/$annotationId", Annotation)

        then: 'the show endpoint shows the update'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        GET("$DATAMODELS_PATH/$dataModelId$ANNOTATION_PATH/$annotationId$ANNOTATION_PATH/$childAnnotationId", Annotation)

        then: 'the show endpoint shows the update'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

}
