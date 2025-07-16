package org.maurodata.datamodel

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
class DataModelWithFacetsIntegrationSpec extends CommonDataSpec {

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

    void setup(){
        Folder response = folderApi.create(folder())
        folderId = response.id

        DataModel dataModelResponse = dataModelApi.create(folderId,
              new DataModel(label: 'Test data model',
                            description: 'test description',
                            author: 'test author'))
        dataModelId = dataModelResponse.id

        Metadata metadataResponse = metadataApi.create("dataModel", dataModelId, metadataPayload())
        metadataId = metadataResponse.id

        SummaryMetadata summaryMetadataResponse =
            summaryMetadataApi.create("dataModel", dataModelId, summaryMetadataPayload())
        summaryMetadataId = summaryMetadataResponse.id

        SummaryMetadataReport reportResponse =
            summaryMetadataReportApi.create("dataModel", dataModelId, summaryMetadataId,
                summaryMetadataReport())
        reportId = reportResponse.id

        Annotation annotationResponse = annotationApi.create("dataModel",dataModelId, annotationPayload())
        annotationId = annotationResponse.id

        Annotation childResp = annotationApi.create("dataModel", dataModelId, annotationId,
                annotationPayload('child label', 'child description'))
        childAnnotationId = childResp.id
        String fileContent = "a  very long string the quick brown fox jumped over the dog"

        referenceFile = referenceFileApi.create("dataModel", dataModelId,
                new ReferenceFile(fileName: "test file name",
                    fileSize: fileContent.size(),
                    fileContents: fileContent.bytes,
                    fileType: "text/plain"))
    }

    void 'test get data model with facets - should return all nested facets'() {
        when:
        DataModel retrieved = dataModelApi.show(dataModelId)
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
        DataModel retrieved = dataModelApi.show(dataModelId)

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
        HttpResponse httpResponse = dataModelApi.delete(dataModelId, new DataModel(),true)

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        def response = dataModelApi.show(dataModelId)

        then: 'the show endpoint shows the update'
        !response

        when:
        response = metadataApi.show("dataModel", dataModelId, metadataId)

        then: 'the show endpoint shows the update'
        !response

        when:
        response = summaryMetadataReportApi.show("dataModel", dataModelId, summaryMetadataId, reportId)

        then: 'the show endpoint shows the update'
        !response

        when:
        response = annotationApi.show("dataModel",dataModelId, annotationId)

        then: 'the show endpoint shows the update'
        !response

        when:
        response = annotationApi.getChildAnnotation("dataModel", dataModelId, annotationId, childAnnotationId)

        then: 'the show endpoint shows the update'
        !response

    }

}
