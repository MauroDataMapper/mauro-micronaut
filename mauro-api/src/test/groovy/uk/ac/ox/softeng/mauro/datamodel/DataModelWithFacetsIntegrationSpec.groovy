package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.api.classifier.ClassificationSchemeApi
import uk.ac.ox.softeng.mauro.api.classifier.ClassifierApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.api.datamodel.EnumerationValueApi
import uk.ac.ox.softeng.mauro.api.facet.AnnotationApi
import uk.ac.ox.softeng.mauro.api.facet.MetadataApi
import uk.ac.ox.softeng.mauro.api.facet.ReferenceFileApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

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
