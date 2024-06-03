package uk.ac.ox.softeng.mauro.importexport

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
class FolderJsonImportExportSpec extends CommonDataSpec {
    static String JSON_EXPORTER_NAMESPACE = '/uk.ac.ox.softeng.mauro.plugin.exporter.json'
    static String JSON_EXPORTER_NAME = '/JsonFolderExporterPlugin'
    static String JSON_EXPORTER_VERSION = '/4.0.0'

    @Inject
    EmbeddedApplication<?> application

    @Inject
    ObjectMapper objectMapper

    @Shared
    UUID folderId

    @Shared
    ExportModel exportModel

    JsonSlurper jsonSlurper = new JsonSlurper()

    void 'create dataModel and export'() {
        given:
        folderId = UUID.fromString(POST("$FOLDERS_PATH", [label: 'Test folder']).id as String)
        UUID dataModelId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model']).id as String)
        UUID dataClass1Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class']).id as String)
        UUID dataClass2Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-2', definition: 'second data class']).id as String)
        UUID dataTypeId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'Test data type', domainType: 'PrimitiveType']).id as String)

        Metadata metadataResponse = (Metadata) POST("$FOLDERS_PATH/$folderId$METADATA_PATH", metadataPayload(), Metadata)

        SummaryMetadata summaryMetadataResponse = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataPayload(), SummaryMetadata)

        SummaryMetadataReport reportResponse = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataResponse.id$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)

        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH", annotationPayload(), Annotation)
        Annotation childAnnotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id$ANNOTATION_PATH", annotationPayload('childLabel','child-description'), Annotation)

        CodeSet codeSet = (CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)

        Terminology terminology = (Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology(), Terminology)

        TermRelationshipType termRelationshipType = (TermRelationshipType) POST("$TERMINOLOGIES_PATH/$terminology.id$TERM_RELATIONSHIP_TYPES",
                termRelationshipType(), TermRelationshipType)

        Term term = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", term(), Term )

        when:
        String json = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION", String)

        then:
        json
        Map parsedJson = jsonSlurper.parseText(json) as Map
        parsedJson.exportMetadata
        parsedJson.folder
        parsedJson.folder.dataModels.size() == 1
        parsedJson.folder.dataModels[0].id == dataModelId.toString()
        parsedJson.folder.dataModels[0].label == 'Test data model'
        parsedJson.folder.dataModels[0].dataTypes.size() == 1
        parsedJson.folder.dataModels[0].dataTypes[0].label == 'Test data type'
        parsedJson.folder.dataModels[0].dataTypes[0].id == dataTypeId.toString()
        parsedJson.folder.dataModels[0].dataClasses.size() == 2
        parsedJson.folder.dataModels[0].dataClasses[0].label ==  'TEST-1'
        parsedJson.folder.dataModels[0].dataClasses[0].id == dataClass1Id.toString()
        parsedJson.folder.dataModels[0].dataClasses[1].label ==  'TEST-2'
        parsedJson.folder.dataModels[0].dataClasses[1].id == dataClass2Id.toString()

        parsedJson.folder.metadata.size() == 1
        parsedJson.folder.metadata[0].id == metadataResponse.id.toString()
        parsedJson.folder.summaryMetadata.size() == 1
        parsedJson.folder.summaryMetadata[0].id == summaryMetadataResponse.id.toString()
        parsedJson.folder.summaryMetadata[0].summaryMetadataReports.size() == 1
        parsedJson.folder.summaryMetadata[0].summaryMetadataReports[0].id == reportResponse.id.toString()

        parsedJson.folder.annotations.size() == 1
        parsedJson.folder.annotations[0].id == annotation.id.toString()
        parsedJson.folder.annotations[0].childAnnotations.size() == 1
        parsedJson.folder.annotations[0].childAnnotations[0].id == childAnnotation.id.toString()

        parsedJson.folder.terminologies.size() == 1
        parsedJson.folder.terminologies[0].id == terminology.id.toString()
        parsedJson.folder.terminologies[0].termRelationshipTypes.size() == 1
        parsedJson.folder.terminologies[0].termRelationshipTypes[0].id == termRelationshipType.id.toString()
        parsedJson.folder.terminologies[0].terms.size() == 1
        parsedJson.folder.terminologies[0].terms[0].id == term.id.toString()

        parsedJson.folder.codeSets.size() == 1
        parsedJson.folder.codeSets[0].id == codeSet.id.toString()
    }
}
