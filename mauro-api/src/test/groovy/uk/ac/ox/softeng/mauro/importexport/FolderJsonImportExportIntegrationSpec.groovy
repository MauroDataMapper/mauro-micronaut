package uk.ac.ox.softeng.mauro.importexport

import groovy.json.JsonSlurper
import io.micronaut.http.MediaType
import io.micronaut.http.server.multipart.MultipartBody
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
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.terminology.*
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

    @Shared
    UUID codeSetId

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
        codeSetId = codeSet.id

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
        List<DataClass> dataClasses = parsedJson.folder.dataModels[0].dataClasses
        dataClasses.label.sort().collect { it.toString() } == ['TEST-1', 'TEST-2']
        dataClasses.id.sort().collect { it.toString() } == [ dataClass1Id.toString(), dataClass2Id.toString()].sort()

        parsedJson.folder.metadata.size() == 1
        parsedJson.folder.metadata[0].id == metadataResponse.id.toString()
        parsedJson.folder.summaryMetadata.size() == 1
        parsedJson.folder.summaryMetadata[0].id == summaryMetadataResponse.id.toString()
        parsedJson.folder.summaryMetadata[0].summaryMetadataReports.size() == 1
        parsedJson.folder.summaryMetadata[0].summaryMetadataReports[0].id == reportResponse.id.toString()

        parsedJson.folder.annotations.size() == 1
        parsedJson.folder.annotations[0].id == annotation.id.toString()
        List<Annotation> childAnnotations = parsedJson.folder.annotations[0].childAnnotations
        childAnnotations.size() == 1
        childAnnotations[0].id == childAnnotation.id.toString()

        parsedJson.folder.terminologies.size() == 1
        parsedJson.folder.terminologies[0].id == terminology.id.toString()

        List<TermRelationshipType> termRelationshipTypes = parsedJson.folder.terminologies[0].termRelationshipTypes
        termRelationshipTypes.size() == 1
        termRelationshipTypes[0].id == termRelationshipType.id.toString()
        List<Terminology> terminologies = parsedJson.folder.terminologies
        terminologies.size() == 1
        terminologies[0].terms.size() == 1
        terminologies[0].terms[0].id == term.id.toString()

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


    void 'test consume export folders  - should import'() {
        given:
        UUID dataClassId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'TEST-1', definition: 'first data class']).id as String)
        UUID dataTypeId = UUID.fromString(POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'Test data type', domainType: 'PrimitiveType']).id as String)

        UUID childFolderId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$FOLDERS_PATH", [label: 'child folder'],).id as String)

        UUID childCodeSetId = UUID.fromString(POST("$FOLDERS_PATH/$childFolderId$CODE_SET_PATH", [label: 'codeset in child folder'],).id as String)

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

        Term targetTerm = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", [code: 'target code', definition: 'target term'], Term)

       (TermRelationship) POST("/terminologies/$terminology.id/termRelationships",
                [
                        relationshipType: [id: termRelationshipType.id],
                        sourceTerm      : [id: sourceTerm.id],
                        targetTerm      : [id: targetTerm.id]], TermRelationship)


        String exportJson = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION", String)

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
        ListResponse<DataModel> importedDataModelListResponse = (ListResponse<DataModel>) GET("$FOLDERS_PATH/$importedFolderId$DATAMODELS_PATH", ListResponse, DataModel)
        then:
        importedDataModelListResponse
        importedDataModelListResponse.items.size() == 1
        importedDataModelListResponse.items[0].id != dataModelId
        String importedDataModelId = importedDataModelListResponse.items[0].id
        when:
        ListResponse<DataType> importedDataTypesListResponse = (ListResponse<DataType>) GET("$DATAMODELS_PATH/$importedDataModelId$DATATYPES_PATH", ListResponse, DataType)
        then:
        importedDataTypesListResponse
        importedDataTypesListResponse.items.size() == 1
        importedDataTypesListResponse.items[0].id != dataTypeId

        when:
        ListResponse<DataClass> importedDataClassesListResponse = (ListResponse<DataClass>) GET("$DATAMODELS_PATH/$importedDataModelId$DATACLASSES_PATH", ListResponse, DataClass)
        then:
        importedDataClassesListResponse.items.size() == 1
        importedDataClassesListResponse.items[0].label == 'TEST-1'
        importedDataClassesListResponse.items[0].id != dataClassId

        when:
        ListResponse<SummaryMetadata> importedSummaryMetadataResponse = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$importedFolderId$SUMMARY_METADATA_PATH", ListResponse, SummaryMetadata)
        then:
        importedSummaryMetadataResponse
        importedSummaryMetadataResponse.items.size() == 1
        importedSummaryMetadataResponse.items[0].id != summaryMetadataResponse.id

        when:
        ListResponse<SummaryMetadataReport> importedReportResponse = (ListResponse<Metadata>) GET("$FOLDERS_PATH/$importedFolderId$METADATA_PATH", ListResponse, Metadata)
        then:
        importedReportResponse
        importedReportResponse.items.size() == 1
        importedReportResponse.items[0].id != reportResponse.id

        when:
        ListResponse<Folder> importedChildFolders = (ListResponse<Folder>) GET("$FOLDERS_PATH/$importedFolderId$FOLDERS_PATH", ListResponse, Folder)
        then:
        importedChildFolders
        importedChildFolders.items.size() == 1
        String importedChildFolderId = importedChildFolders.items[0].id

        when:
        ListResponse<CodeSet> importedChildCodeSet = (ListResponse<CodeSet>) GET("$FOLDERS_PATH/$importedChildFolderId$CODE_SET_PATH", ListResponse, CodeSet)
        then:
        importedChildCodeSet
        importedChildCodeSet.items.size() == 1
        importedChildCodeSet.items[0].id != childCodeSetId

        when:
        ListResponse<Metadata> importedMetadataResponse = (ListResponse<Metadata>) GET("$FOLDERS_PATH/$importedFolderId$METADATA_PATH", ListResponse, Metadata)

        then:
        importedMetadataResponse
        importedMetadataResponse.items.size() == 1
        importedMetadataResponse.items[0].id != metadataResponse.id

        when:
        ListResponse<Annotation> importedAnnotationResponse = (ListResponse<Annotation>) GET("$FOLDERS_PATH/$importedFolderId$ANNOTATION_PATH", ListResponse, Annotation)

        then:
        importedAnnotationResponse
        importedAnnotationResponse.items.size() == 1
        UUID importedAnnotationId = importedAnnotationResponse.items[0].id
        importedAnnotationResponse.items[0]?.id != annotation.id
        importedAnnotationResponse.items[0]?.childAnnotations
        importedAnnotationResponse.items[0]?.childAnnotations?.size() == 1
        importedAnnotationResponse.items[0]?.childAnnotations[0].parentAnnotationId == importedAnnotationId
        importedAnnotationResponse.items[0]?.childAnnotations[0].id != childAnnotation.id

        when:
        ListResponse<CodeSet> importedCodeSetResponse = (ListResponse<CodeSet>) GET("$FOLDERS_PATH/$importedFolderId$CODE_SET_PATH", ListResponse, CodeSet)

        then:
        importedCodeSetResponse
        importedCodeSetResponse.items.size() == 1
        importedCodeSetResponse.items[0].id != codeSet.id

        when:
        ListResponse<Terminology> importedTerminologyResponse = (ListResponse<Terminology>) GET("$FOLDERS_PATH/$importedFolderId$TERMINOLOGIES_PATH", ListResponse, Terminology)

        then:
        importedTerminologyResponse
        importedTerminologyResponse.items.size() == 1
        String importedTerminologyIdString = importedTerminologyResponse.items[0].id
        importedTerminologyIdString != terminology.id

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypeResponse =
                (ListResponse<TermRelationshipType>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERM_RELATIONSHIP_TYPES", ListResponse, TermRelationshipType)

        then:
        importedTermRelationshipTypeResponse
        importedTermRelationshipTypeResponse.items.size() == 1
        UUID importedTermRelationshipTypeIdString = importedTermRelationshipTypeResponse.items[0].id
        importedTermRelationshipTypeIdString != termRelationshipType.id

        when:
        ListResponse<Term> importedTermsResponse = (ListResponse<Term>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERMS_PATH", ListResponse, Term)

        then:
        importedTermsResponse
        importedTermsResponse.items.size() == 2

        importedTermsResponse.items.code.sort().collect { it.toString() } == ['source code', 'target code']
        importedTermsResponse.items.id.sort().collect { it.toString() } != ["${sourceTerm.id}", "${targetTerm.id}"]

        when:
        ListResponse<TermRelationship> importedTermRelationship = (ListResponse<TermRelationship>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERM_RELATIONSHIP_PATH", ListResponse, TermRelationship)
        then:
        importedTermRelationship
        importedTermRelationship.items.size() == 1
        importedTermRelationship.items[0].id != termRelationshipType.id
        importedTermRelationship.items[0].sourceTerm.code == 'source code'
        importedTermRelationship.items[0].targetTerm.code == 'target code'
        importedTermRelationship.items[0].relationshipType.id == importedTermRelationshipTypeIdString
    }


    void 'test consume export folder- folder is not parent - should import'() {
        given:

        UUID childFolderId = UUID.fromString(POST("$FOLDERS_PATH/$folderId$FOLDERS_PATH", [label: 'child folder'],).id as String)

        UUID childCodeSetId = UUID.fromString(POST("$FOLDERS_PATH/$childFolderId$CODE_SET_PATH", [label: 'codeset in child folder'],).id as String)

        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH", annotationPayload(), Annotation)
        Annotation childAnnotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id$ANNOTATION_PATH", annotationPayload('childLabel', 'child-description'), Annotation)

        CodeSet codeSet = (CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)

        Terminology terminology = (Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology(), Terminology)

        TermRelationshipType termRelationshipType = (TermRelationshipType) POST("$TERMINOLOGIES_PATH/$terminology.id$TERM_RELATIONSHIP_TYPES",
                termRelationshipType(), TermRelationshipType)

        Term sourceTerm = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", [code: 'source code', definition: 'source term'], Term)

        Term targetTerm = (Term) POST("$TERMINOLOGIES_PATH/$terminology.id$TERMS_PATH", [code: 'target code', definition: 'target term'], Term)

        (TermRelationship) POST("/terminologies/$terminology.id/termRelationships",
                [
                        relationshipType: [id: termRelationshipType.id],
                        sourceTerm      : [id: sourceTerm.id],
                        targetTerm      : [id: targetTerm.id]], TermRelationship)


        String exportJson = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION", String)

        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', childFolderId.toString())
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
        ListResponse<Folder> importedChildFolders = (ListResponse<Folder>) GET("$FOLDERS_PATH/$importedFolderId$FOLDERS_PATH", ListResponse, Folder)
        then:
        importedChildFolders
        importedChildFolders.items.size() == 1
        String importedChildFolderId = importedChildFolders.items[0].id

        when:
        ListResponse<CodeSet> importedChildCodeSet = (ListResponse<CodeSet>) GET("$FOLDERS_PATH/$importedChildFolderId$CODE_SET_PATH", ListResponse, CodeSet)
        then:
        importedChildCodeSet
        importedChildCodeSet.items.size() == 1
        importedChildCodeSet.items[0].id != childCodeSetId


        when:
        ListResponse<Annotation> importedAnnotationResponse = (ListResponse<Annotation>) GET("$FOLDERS_PATH/$importedFolderId$ANNOTATION_PATH", ListResponse, Annotation)

        then:
        importedAnnotationResponse
        importedAnnotationResponse.items.size() == 1
        UUID importedAnnotationId = importedAnnotationResponse.items[0].id
        importedAnnotationResponse.items[0]?.id != annotation.id
        importedAnnotationResponse.items[0]?.childAnnotations
        importedAnnotationResponse.items[0]?.childAnnotations?.size() == 1
        importedAnnotationResponse.items[0]?.childAnnotations[0].parentAnnotationId == importedAnnotationId
        importedAnnotationResponse.items[0]?.childAnnotations[0].id != childAnnotation.id

        when:
        ListResponse<CodeSet> importedCodeSetResponse = (ListResponse<CodeSet>) GET("$FOLDERS_PATH/$importedFolderId$CODE_SET_PATH", ListResponse, CodeSet)

        then:
        importedCodeSetResponse
        importedCodeSetResponse.items.size() == 1
        importedCodeSetResponse.items[0].id != codeSet.id

        when:
        ListResponse<Terminology> importedTerminologyResponse = (ListResponse<Terminology>) GET("$FOLDERS_PATH/$importedFolderId$TERMINOLOGIES_PATH", ListResponse, Terminology)

        then:
        importedTerminologyResponse
        importedTerminologyResponse.items.size() == 1
        String importedTerminologyIdString = importedTerminologyResponse.items[0].id
        importedTerminologyIdString != terminology.id

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypeResponse =
                (ListResponse<TermRelationshipType>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERM_RELATIONSHIP_TYPES", ListResponse, TermRelationshipType)

        then:
        importedTermRelationshipTypeResponse
        importedTermRelationshipTypeResponse.items.size() == 1
        UUID importedTermRelationshipTypeIdString = importedTermRelationshipTypeResponse.items[0].id
        importedTermRelationshipTypeIdString != termRelationshipType.id

        when:
        ListResponse<Term> importedTermsResponse = (ListResponse<Term>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERMS_PATH", ListResponse, Term)

        then:
        importedTermsResponse
        importedTermsResponse.items.size() == 2

        importedTermsResponse.items.code.sort().collect { it.toString() } == ['source code', 'target code']
        importedTermsResponse.items.id.sort().collect { it.toString() } != ["${sourceTerm.id}", "${targetTerm.id}"]

        when:
        ListResponse<TermRelationship> importedTermRelationship = (ListResponse<TermRelationship>) GET("$TERMINOLOGIES_PATH/$importedTerminologyIdString$TERM_RELATIONSHIP_PATH", ListResponse, TermRelationship)
        then:
        importedTermRelationship
        importedTermRelationship.items.size() == 1
        importedTermRelationship.items[0].id != termRelationshipType.id
        importedTermRelationship.items[0].sourceTerm.code == 'source code'
        importedTermRelationship.items[0].targetTerm.code == 'target code'
        importedTermRelationship.items[0].relationshipType.id == importedTermRelationshipTypeIdString
    }

    void 'export and import folder with two terminologies with overlapping codes'() {
        given:
        // create two terminologies each with different Terms with code TEST
        UUID folderId = UUID.fromString(POST("$FOLDERS_PATH", [label: 'Two terminologies folder']).id)
        UUID terminology1Id = UUID.fromString(POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", [label: 'First Terminology']).id)
        UUID term1Id = UUID.fromString(POST("$TERMINOLOGIES_PATH/$terminology1Id$TERMS_PATH", [code: 'TEST', definition: 'first term']).id)
        UUID terminology2Id = UUID.fromString(POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", [label: 'Second Terminology']).id)
        UUID term2Id = UUID.fromString(POST("$TERMINOLOGIES_PATH/$terminology2Id$TERMS_PATH", [code: 'TEST', definition: 'second term']).id)

        // also create two different term relationship types with label TEST
        UUID termRelationshipType1Id = UUID.fromString(POST("$TERMINOLOGIES_PATH/$terminology1Id$TERM_RELATIONSHIP_TYPES", [label: 'TEST', childRelationship: true]).id)
        UUID termRelationshipType2Id = UUID.fromString(POST("$TERMINOLOGIES_PATH/$terminology2Id$TERM_RELATIONSHIP_TYPES", [label: 'TEST', childRelationship: false]).id)
        POST("$TERMINOLOGIES_PATH/$terminology1Id$TERM_RELATIONSHIP_PATH", [
            relationshipType: [id: termRelationshipType1Id],
            sourceTerm: [id: term1Id],
            targetTerm: [id: term1Id]
        ])
        POST("$TERMINOLOGIES_PATH/$terminology2Id$TERM_RELATIONSHIP_PATH", [
            relationshipType: [id: termRelationshipType2Id],
            sourceTerm: [id: term2Id],
            targetTerm: [id: term2Id]
        ])

        when:
        Map<String, Object> export = GET("$FOLDERS_PATH/$folderId$EXPORT_PATH$JSON_EXPORTER_NAMESPACE$JSON_EXPORTER_NAME$JSON_EXPORTER_VERSION")

        then:
        export.folder.terminologies.size() == 2
        export.folder.terminologies.find {it.label == 'First Terminology'}.terms.first().code == 'TEST'
        export.folder.terminologies.find {it.label == 'First Terminology'}.terms.first().definition == 'first term'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.terms.first().code == 'TEST'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.terms.first().definition == 'second term'
        export.folder.terminologies.find {it.label == 'First Terminology'}.termRelationshipTypes.first().label == 'TEST'
        export.folder.terminologies.find {it.label == 'First Terminology'}.termRelationshipTypes.first().childRelationship == true
        export.folder.terminologies.find {it.label == 'Second Terminology'}.termRelationshipTypes.first().label == 'TEST'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.termRelationshipTypes.first().childRelationship == false
        export.folder.terminologies.find {it.label == 'First Terminology'}.termRelationships.first().sourceTerm == 'TEST'
        export.folder.terminologies.find {it.label == 'First Terminology'}.termRelationships.first().targetTerm == 'TEST'
        export.folder.terminologies.find {it.label == 'First Terminology'}.termRelationships.first().relationshipType == 'TEST'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.termRelationships.first().sourceTerm == 'TEST'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.termRelationships.first().targetTerm == 'TEST'
        export.folder.terminologies.find {it.label == 'Second Terminology'}.termRelationships.first().relationshipType == 'TEST'

        when:
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(export))
            .build()
        ListResponse<Folder> response = POST("$FOLDERS_PATH$IMPORT_PATH$JSON_IMPORTER_NAMESPACE$JSON_IMPORTER_NAME$JSON_IMPORTER_VERSION", importRequest) as ListResponse
        UUID importedFolderId = UUID.fromString(response.items.first().id)

        then:
        response.count == 1
        response.items.size() == 1
        response.items.first().label == 'Two terminologies folder'
        importedFolderId

        when:
        ListResponse<Terminology> importedTerminologies = GET("$FOLDERS_PATH/$importedFolderId$TERMINOLOGIES_PATH", ListResponse, Terminology)
        UUID importedTerminology1Id = importedTerminologies.items.find {it.label == 'First Terminology'}.id
        UUID importedTerminology2Id = importedTerminologies.items.find {it.label == 'Second Terminology'}.id

        then:
        importedTerminologies.count == 2
        importedTerminologies.items.find {it.label == 'First Terminology'}
        importedTerminologies.items.find {it.label == 'Second Terminology'}

        when:
        ListResponse<Term> importedTerms = GET("$TERMINOLOGIES_PATH/$importedTerminology1Id$TERMS_PATH", ListResponse, Term)

        then:
        importedTerms.count == 1
        importedTerms.items.first().code == 'TEST'
        importedTerms.items.first().definition == 'first term'

        when:
        importedTerms = GET("$TERMINOLOGIES_PATH/$importedTerminology2Id$TERMS_PATH", ListResponse, Term)

        then:
        importedTerms.count == 1
        importedTerms.items.first().code == 'TEST'
        importedTerms.items.first().definition == 'second term'

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypes = GET("$TERMINOLOGIES_PATH/$importedTerminology1Id$TERM_RELATIONSHIP_TYPES", ListResponse, TermRelationshipType)

        then:
        importedTermRelationshipTypes.count == 1
        importedTermRelationshipTypes.items.first().label == 'TEST'
        importedTermRelationshipTypes.items.first().childRelationship == true

        when:
        importedTermRelationshipTypes = GET("$TERMINOLOGIES_PATH/$importedTerminology2Id$TERM_RELATIONSHIP_TYPES", ListResponse, TermRelationshipType)

        then:
        importedTermRelationshipTypes.count == 1
        importedTermRelationshipTypes.items.first().label == 'TEST'
        importedTermRelationshipTypes.items.first().childRelationship == false

        when:
        ListResponse<TermRelationship> importedTermRelationships = (ListResponse<TermRelationship>) GET("$TERMINOLOGIES_PATH/$importedTerminology1Id$TERM_RELATIONSHIP_PATH", ListResponse, TermRelationship)

        then:
        importedTermRelationships.count == 1
        importedTermRelationships.items.first().sourceTerm.code == 'TEST'
        importedTermRelationships.items.first().targetTerm.code == 'TEST'
        importedTermRelationships.items.first().relationshipType.label == 'TEST'

        when:
        importedTermRelationships = GET("$TERMINOLOGIES_PATH/$importedTerminology2Id$TERM_RELATIONSHIP_PATH", ListResponse)

        then:
        importedTermRelationships.count == 1
        importedTermRelationships.items.first().sourceTerm.code == 'TEST'
        importedTermRelationships.items.first().targetTerm.code == 'TEST'
        importedTermRelationships.items.first().relationshipType.label == 'TEST'
    }
}
