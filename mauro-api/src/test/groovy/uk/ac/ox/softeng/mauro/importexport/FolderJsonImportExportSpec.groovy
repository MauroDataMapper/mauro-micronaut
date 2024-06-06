package uk.ac.ox.softeng.mauro.importexport

import groovy.json.JsonSlurper
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
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
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql","classpath:sql/tear-down-folders.sql"], phase = Sql.Phase.AFTER_EACH)
class FolderJsonImportExportSpec extends CommonDataSpec {
    static String JSON_EXPORTER_NAMESPACE = '/uk.ac.ox.softeng.mauro.plugin.exporter.json'
    static String JSON_EXPORTER_NAME = '/JsonFolderExporterPlugin'
    static String JSON_EXPORTER_VERSION = '/4.0.0'

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    UUID dataModelId


    JsonSlurper jsonSlurper = new JsonSlurper()

    void setup() {
        folderId = UUID.fromString ( POST("$FOLDERS_PATH", [label: 'Folder with SummaryMetadata'],).id as String)
        dataModelId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model']).id as String)
    }

    void 'create folder, dataModels, metadata, summaryMetadata, summaryMetadataReports, annotations, terminology, codeset and export'() {
        given:
        UUID dataClass1Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class']).id as String)
        UUID dataClass2Id = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-2', definition: 'second data class']).id as String)
        UUID dataTypeId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'Test data type', domainType: 'PrimitiveType']).id as String)

        Metadata metadataResponse = (Metadata) POST("$FOLDERS_PATH/$folderId$METADATA_PATH", metadataPayload(), Metadata)

        SummaryMetadata summaryMetadataResponse = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataPayload(), SummaryMetadata)

        SummaryMetadataReport reportResponse = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataResponse.id$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)

        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH", annotationPayload(), Annotation)
        Annotation childAnnotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id$ANNOTATION_PATH", annotationPayload('childLabel', 'child-description'), Annotation)

        CodeSet codeSet = (CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)

        Terminology terminology = (Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology(), Terminology)

        TermRelationshipType termRelationshipType = (TermRelationshipType) POST("$TERMINOLOGIES_PATH/$terminology.id$TERM_RELATIONSHIP_TYPES",
                termRelationshipType(), TermRelationshipType)

        Term term = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", term(), Term)

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
        parsedJson.folder.dataModels[0].dataClasses[0].label == 'TEST-1'
        parsedJson.folder.dataModels[0].dataClasses[0].id == dataClass1Id.toString()
        parsedJson.folder.dataModels[0].dataClasses[1].label == 'TEST-2'
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

    void 'get export folder -should export folder, with nested data and deep nesting of child folders'() {
        given:
        UUID childFolderId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$FOLDERS_PATH", [label: 'Test child folder 1st level']).id as String)
        UUID nestedChildFolderId = UUID.fromString(POST("$FOLDERS_PATH/$childFolderId$FOLDERS_PATH", [label: 'Test nested child 2nd level folder']).id as String)

        and:
        UUID childDataModelId = UUID.fromString(POST("$FOLDERS_PATH/$childFolderId$DATAMODELS_PATH", [label: 'Test child 1st level folder']).id as String)
        UUID nestedChildDataModelId = UUID.fromString(POST("$FOLDERS_PATH/$nestedChildFolderId$DATAMODELS_PATH", [label: 'Test nested child 2nd level folder']).id as String)

        UUID childDataModelTypeId = UUID.fromString(POST("$DATAMODELS_PATH/$childDataModelId$DATATYPES_PATH", [label: 'Test data type childData Model', domainType: 'PrimitiveType']).id as String)

        when:
        String json = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION", String)
        then:
        json

        Map parsedJson = jsonSlurper.parseText(json) as Map
        parsedJson.exportMetadata
        parsedJson.folder
        parsedJson.folder.dataModels.size() == 1
        parsedJson.folder.dataModels[0].id == dataModelId.toString()

        parsedJson.folder.childFolders.size() == 1
        parsedJson.folder.childFolders[0].id == childFolderId.toString()
        parsedJson.folder.childFolders[0].dataModels.size() == 1
        parsedJson.folder.childFolders[0].dataModels[0].id == childDataModelId.toString()
        parsedJson.folder.childFolders[0].dataModels[0].dataTypes.size() == 1
        parsedJson.folder.childFolders[0].dataModels[0].dataTypes[0].id == childDataModelTypeId.toString()

        parsedJson.folder.childFolders[0].childFolders.size() == 1
        parsedJson.folder.childFolders[0].childFolders[0].id == nestedChildFolderId.toString()
        parsedJson.folder.childFolders[0].childFolders[0].dataModels.size() == 1
        parsedJson.folder.childFolders[0].childFolders[0].dataModels[0].id == nestedChildDataModelId.toString()
    }
}
