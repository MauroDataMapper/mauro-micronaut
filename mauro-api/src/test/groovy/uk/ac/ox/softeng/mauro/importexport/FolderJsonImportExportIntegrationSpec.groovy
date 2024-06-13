package uk.ac.ox.softeng.mauro.importexport

import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-annotation.sql", "classpath:sql/tear-down-metadata.sql",
        "classpath:sql/tear-down-summary-metadata.sql", "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class FolderJsonImportExportIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    JsonSlurper jsonSlurper = new JsonSlurper()

    void setup() {
        folderId = UUID.fromString(POST("$FOLDERS_PATH", [label: 'Folder top level'],).id as String)
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
        parsedJson.folders
        parsedJson.folders.dataModels.size() == 1
        parsedJson.folders.dataModels[0].id[0] == dataModelId.toString()
        parsedJson.folders.dataModels[0].label[0] == 'Test data model'
        parsedJson.folders.dataModels[0].dataTypes.size() == 1
        parsedJson.folders.dataModels[0].dataTypes[0].label[0] == 'Test data type'
        parsedJson.folders.dataModels[0].dataTypes[0].id[0] == dataTypeId.toString()
        parsedJson.folders.dataModels[0].dataClasses.size() == 1
        List<DataClass> dataClasses = parsedJson.folders.dataModels[0].dataClasses[0]
        dataClasses[0].label == 'TEST-1'
        dataClasses[0].id == dataClass1Id.toString()

        dataClasses[1].label == 'TEST-2'
        dataClasses[1].id == dataClass2Id.toString()

        parsedJson.folders.metadata.size() == 1
        parsedJson.folders.metadata[0].id[0] == metadataResponse.id.toString()
        parsedJson.folders.summaryMetadata.size() == 1
        parsedJson.folders.summaryMetadata[0].id[0] == summaryMetadataResponse.id.toString()
        parsedJson.folders.summaryMetadata[0].summaryMetadataReports.size() == 1
        parsedJson.folders.summaryMetadata[0].summaryMetadataReports[0].id[0] == reportResponse.id.toString()

        parsedJson.folders.annotations.size() == 1
        parsedJson.folders.annotations[0].id[0] == annotation.id.toString()
        List<Annotation> childAnnotations = parsedJson.folders.annotations[0].childAnnotations[0]
        childAnnotations.size() == 1
        childAnnotations[0].id == childAnnotation.id.toString()

        parsedJson.folders.terminologies.size() == 1
        parsedJson.folders.terminologies[0].id[0] == terminology.id.toString()

        List<TermRelationshipType> termRelationshipTypes = parsedJson.folders.terminologies[0].termRelationshipTypes
        termRelationshipTypes.size() == 1
        termRelationshipTypes[0].id[0] == termRelationshipType.id.toString()
        List<Terminology> terminologies = parsedJson.folders.terminologies
        terminologies.size() == 1
        terminologies[0].terms.size() == 1
        terminologies[0].terms[0].id[0] == term.id.toString()

        parsedJson.folders.codeSets.size() == 1
        parsedJson.folders.codeSets[0].id[0] == codeSet.id.toString()
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
        parsedJson.folders
        parsedJson.folders.dataModels.size() == 1
        parsedJson.folders.dataModels[0].id[0] == dataModelId.toString()

        parsedJson.folders.childFolders.size() == 1
        parsedJson.folders.childFolders[0].id[0] == childFolderId.toString()
        parsedJson.folders.childFolders[0].dataModels.size() == 1
        parsedJson.folders.childFolders[0].dataModels[0].id[0] == childDataModelId.toString()
        parsedJson.folders.childFolders[0].dataModels[0].dataTypes.size() == 1
        parsedJson.folders.childFolders[0].dataModels[0].dataTypes[0].id[0] == childDataModelTypeId.toString()

        parsedJson.folders.childFolders[0].childFolders.size() == 1
        parsedJson.folders.childFolders[0].childFolders[0].id[0] == nestedChildFolderId.toString()
        parsedJson.folders.childFolders[0].childFolders[0].dataModels.size() == 1
        parsedJson.folders.childFolders[0].childFolders[0].dataModels[0].id[0] == nestedChildDataModelId.toString()
    }


    void 'test consume export folders  - should import'() {
        given:
        UUID dataClassId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class']).id as String)
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

        Term sourceTerm = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", [code: 'source code', definition: 'source term'], Term)

        Term targetTerm = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", [code : 'target code', definition: 'target term'], Term )

        TermRelationship termRelationshipResponse = (TermRelationship) POST("/terminologies/$terminology.id/termRelationships",
                [
                        relationshipType: [id: termRelationshipType.id],
                        sourceTerm      : [id: sourceTerm.id],
                        targetTerm      : [id: targetTerm.id]], TermRelationship)


        String exportJson = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION", String)
        exportJson


        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, exportJson.bytes)
                .build()

        when:
        ListResponse<Folder> response = (ListResponse<Folder>) POST("$FOLDERS_PATH$IMPORT_PATH$JSON_IMPORTER_NAMESPACE$JSON_IMPORTER_NAME$JSON_IMPORTER_VERSION", importRequest)

        then:
        response
        UUID importedFolderId = UUID.fromString(response.items.id.first() as String)

        when:
        Folder importedFolder = (Folder) GET("$FOLDERS_PATH/$importedFolderId", Folder)

        then:
        importedFolder

        when:
        ListResponse<DataModel> importedDataModelListResponse = (ListResponse<DataModel>) GET("$FOLDERS_PATH/$importedFolderId$DATAMODELS_PATH", ListResponse<DataModel>)
        then:
        importedDataModelListResponse
        importedDataModelListResponse.items.size() == 1
        importedDataModelListResponse.items[0].id != dataModelId
        String importedDataModelId = importedDataModelListResponse.items[0].id
        when:
        ListResponse<DataType> importedDataTypesListResponse = (ListResponse<DataType>) GET("$DATAMODELS_PATH/$importedDataModelId$DATATYPES_PATH", ListResponse<DataType>)
        then:
        importedDataTypesListResponse
        importedDataTypesListResponse.items.size() == 1
        importedDataTypesListResponse.items[0].id != dataTypeId

        when:
        ListResponse<DataClass> importedDataClassesListResponse = (ListResponse<DataClass>) GET("$DATAMODELS_PATH/$importedDataModelId$DATACLASSES_PATH", ListResponse<DataClass>)
        then:
        importedDataClassesListResponse.items.size() == 1
        importedDataClassesListResponse.items[0].label == 'TEST-1'
        importedDataClassesListResponse.items[0].id != dataClassId

        // when:
//        ListResponse<SummaryMetadata> importedSummaryMetadataResponse = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$importedFolderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)
//        then:
//        importedSummaryMetadataResponse
//        println("imported summarymetadata items: ${importedSummaryMetadataResponse.items.size}")
//
//        println("Get summaryMetadata for new folderid: $importedFolderId, oldFolderId; $folderId`")
//        when:
//        importedSummaryMetadataResponse = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$importedFolderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)
//        importedSummaryMetadataResponse = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$importedFolderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)
//        then:
//        importedSummaryMetadataResponse
//        importedSummaryMetadataResponse.items.size() == 1
////        summaryMetadataResp.items.size() == 1
////        summaryMetadataResp.items[0].id  == summaryMetadataResponse.id.toString()
//
//        when:
//        ListResponse<Metadata> importedMetadataResponse = (ListResponse<Metadata>) GET("$FOLDERS_PATH/$importedFolderId$METADATA_PATH", ListResponse<Metadata>)
//        then:
//        importedMetadataResponse
//        importedMetadataResponse.items.size() == 1
//        importedMetadataResponse.items[0].id  != metadataResponse.id


//        when:
//        ListResponse<Annotation> importedAnnotationResponse = (ListResponse<Annotation>) GET("$FOLDERS_PATH/$importedFolderId$ANNOTATION_PATH", ListResponse<Annotation>)
//
//        then:
//        importedAnnotationResponse
//        importedAnnotationResponse.items.size() == 1

        when:
        ListResponse<CodeSet> importedCodeSetResponse = (ListResponse<CodeSet>) GET("$FOLDERS_PATH/$importedFolderId$CODE_SET_PATH", ListResponse<CodeSet>)

        then:
        importedCodeSetResponse
        importedCodeSetResponse.items.size() == 1
        importedCodeSetResponse.items[0].id != codeSet.id

        when:
        ListResponse<Terminology> importedTerminologyResponse = (ListResponse<Terminology>)GET("$FOLDERS_PATH/$importedFolderId$TERMINOLOGIES_PATH", ListResponse<Terminology>)

        then:
        importedTerminologyResponse
        importedTerminologyResponse.items.size() == 1
        def importedTerminologyId = importedTerminologyResponse.items[0].id
        importedTerminologyId != terminology.id

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypeResponse = (ListResponse<TermRelationshipType>) GET("$TERMINOLOGIES_PATH/$importedTerminologyId$TERM_RELATIONSHIP_TYPES", ListResponse<TermRelationshipType>)

        then:
        importedTermRelationshipTypeResponse
        importedTermRelationshipTypeResponse.items.size() == 1
        String importedTermRelationshipTypeIdString = importedTermRelationshipTypeResponse.items[0].id
        importedTermRelationshipTypeIdString != termRelationshipType.id

        when:
        ListResponse<Term> importedTermsResponse = (ListResponse<Term>) GET("$TERMINOLOGIES_PATH/$importedTerminologyId$TERMS_PATH", ListResponse<Term>)

        then:
        importedTermsResponse
        importedTermsResponse.items.size() == 2

        importedTermsResponse.items.code.sort().collect { it.toString()} == ['source code', 'target code']
        importedTermsResponse.items.id.sort().collect { it.toString()} != ["${sourceTerm.id}", "${targetTerm.id}"]

        when:
        ListResponse<TermRelationship> importedTermRelationship = (ListResponse<TermRelationship>) GET("$TERMINOLOGIES_PATH/$importedTerminologyId$TERM_RELATIONSHIP_PATH", ListResponse<TermRelationship>)
        then:
        importedTermRelationship
        importedTermRelationship.items.size() == 1
        importedTermRelationship.items[0].id != termRelationshipType.id
        importedTermRelationship.items[0].sourceTerm.code  == 'source code'
        importedTermRelationship.items[0].targetTerm.code  == 'target code'
        importedTermRelationship.items[0].relationshipType.id == importedTermRelationshipTypeIdString
    }
}
