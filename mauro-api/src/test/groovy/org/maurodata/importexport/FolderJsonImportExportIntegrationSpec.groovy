package org.maurodata.importexport

import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
class FolderJsonImportExportIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId
    @Shared
    DataType dataType
    @Shared
    UUID dataClass1Id
    @Shared
    UUID dataClass2Id
    @Shared
    UUID dataElementId1
    @Shared
    UUID dataElementId2

    @Shared
    DataModel source
    @Shared
    UUID dataFlowId

    @Shared
    UUID dataClassComponentId
    @Shared
    UUID dataElementComponentId

    @Shared
    UUID codeSetId

    JsonSlurper jsonSlurper = new JsonSlurper()

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Folder top level')).id
        dataModelId = dataModelApi.create(folderId, new DataModel(label: 'Test data model')).id
    }

    void 'create folder, dataModels, metadata, summaryMetadata, summaryMetadataReports, annotations, terminology, codeset and export'() {
        given:
        UUID dataClass1Id = dataClassApi.create(dataModelId, new DataClass(label: 'TEST-1', description: 'first data class')).id
        UUID dataClass2Id = dataClassApi.create(dataModelId, new DataClass(label: 'TEST-2', description: 'second data class')).id
        UUID dataTypeId = dataTypeApi.create(dataModelId, new DataType(label: 'Test data type', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id

        Metadata metadataResponse = metadataApi.create("folder", folderId, metadataPayload())

        SummaryMetadata summaryMetadataResponse = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        SummaryMetadataReport reportResponse = summaryMetadataReportApi.create("folder", folderId, summaryMetadataResponse.id, summaryMetadataReport())

        Annotation annotation = annotationApi.create("folder", folderId, annotationPayload())
        Annotation childAnnotation = annotationApi.create("folder", folderId, annotation.id, annotationPayload('childLabel', 'child-description'))

        CodeSet codeSet = codeSetApi.create(folderId, codeSet())
        codeSetId = codeSet.id

        Terminology terminology = terminologyApi.create(folderId, terminologyPayload())

        TermRelationshipType termRelationshipType = termRelationshipTypeApi.create(terminology.id, termRelationshipType())

        Term term = termApi.create(terminology.id, term())

        when:
        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')

        then:
        exportResponse.body()
        Map parsedJson = jsonSlurper.parseText(new String(exportResponse.body())) as Map
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
        UUID childFolderId = folderApi.create(folderId, new Folder(label: 'Test child folder 1st level')).id
        UUID nestedChildFolderId = folderApi.create(childFolderId, new Folder(label: 'Test nested child 2nd level folder')).id

        and:
        UUID childDataModelId = dataModelApi.create(childFolderId, new DataModel(label: 'Test child 1st level folder')).id
        UUID nestedChildDataModelId = dataModelApi.create(nestedChildFolderId, new DataModel(label: 'Test nested child 2nd level folder')).id

        UUID childDataModelTypeId = dataTypeApi.create(childDataModelId,
            new DataType(label: 'Test data type childData Model', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id

        when:
        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')
        then:
        exportResponse.body()

        Map parsedJson = jsonSlurper.parseText(new String(exportResponse.body())) as Map
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
        UUID dataClassId = dataClassApi.create(dataModelId, new DataClass(label: 'TEST-1', description: 'first data class')).id
        UUID dataTypeId = dataTypeApi.create(dataModelId, new DataType(label: 'Test data type', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id

        UUID childFolderId = folderApi.create(folderId, new Folder(label: 'child folder')).id

        UUID childCodeSetId = codeSetApi.create(childFolderId, new CodeSet(label: 'codeset in child folder')).id

        Metadata metadataResponse = metadataApi.create("folder", folderId, metadataPayload())

        SummaryMetadata summaryMetadataResponse = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        SummaryMetadataReport reportResponse = summaryMetadataReportApi.create("folder", folderId, summaryMetadataResponse.id, summaryMetadataReport())

        Annotation annotation = annotationApi.create("folder", folderId, annotationPayload())
        Annotation childAnnotation = annotationApi.create("folder", folderId, annotation.id, annotationPayload('childLabel', 'child-description'))

        CodeSet codeSet = codeSetApi.create(folderId, codeSet())

        Terminology terminology = terminologyApi.create(folderId, terminologyPayload())

        TermRelationshipType termRelationshipType = termRelationshipTypeApi.create(terminology.id, termRelationshipType())

        Term sourceTerm = termApi.create(terminology.id, new Term(code: 'source code', definition: 'source term'))

        Term targetTerm = termApi.create(terminology.id, new Term(code: 'target code', definition: 'target term'))

        termRelationshipApi.create(terminology.id,
                new TermRelationship(
                        relationshipType: new TermRelationshipType(id: termRelationshipType.id),
                        sourceTerm      : new Term(id: sourceTerm.id),
                        targetTerm      : new Term(id: targetTerm.id)))


        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')


        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', folderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, exportResponse.body())
                .build()

        when:
        ListResponse<Folder> response = folderApi.importModel(importRequest, 'org.maurodata.plugin.importer.json', 'JsonFolderImporterPlugin', '4.0.0')

        then:
        response
        UUID importedFolderId = response.items.id.first()

        when:
        Folder importedFolder = folderApi.show(importedFolderId)

        then:
        importedFolder

        when:
        ListResponse<DataModel> importedDataModelListResponse = dataModelApi.list(importedFolderId)
        then:
        importedDataModelListResponse
        importedDataModelListResponse.items.size() == 1
        importedDataModelListResponse.items[0].id != dataModelId
        UUID importedDataModelId = importedDataModelListResponse.items[0].id
        when:
        ListResponse<DataType> importedDataTypesListResponse = dataTypeApi.list(importedDataModelId)
        then:
        importedDataTypesListResponse
        importedDataTypesListResponse.items.size() == 1
        importedDataTypesListResponse.items[0].id != dataTypeId

        when:
        ListResponse<DataClass> importedDataClassesListResponse = dataClassApi.list(importedDataModelId)
        then:
        importedDataClassesListResponse.items.size() == 1
        importedDataClassesListResponse.items[0].label == 'TEST-1'
        importedDataClassesListResponse.items[0].id != dataClassId

        when:
        ListResponse<Metadata> importedMetadataResponse = metadataApi.list("folder", importedFolderId)
        then:
        importedMetadataResponse
        importedMetadataResponse.items.size() == 1
        importedMetadataResponse.items[0].id != metadataResponse.id

        when:
        ListResponse<SummaryMetadata> importedSummaryMetadataResponse = summaryMetadataApi.list("folder", importedFolderId)
        then:
        importedSummaryMetadataResponse
        importedSummaryMetadataResponse.items.size() == 1
        importedSummaryMetadataResponse.items[0].id != summaryMetadataResponse.id

        when:
        ListResponse<SummaryMetadataReport> importedReportResponse =
            summaryMetadataReportApi.list("folder", importedFolderId, importedSummaryMetadataResponse.items[0].id)
        then:
        importedReportResponse
        importedReportResponse.items.size() == 1
        importedReportResponse.items[0].id != reportResponse.id

        when:
        ListResponse<Folder> importedChildFolders = folderApi.list(importedFolderId)
        then:
        importedChildFolders
        importedChildFolders.items.size() == 1
        UUID importedChildFolderId = importedChildFolders.items[0].id

        when:
        ListResponse<CodeSet> importedChildCodeSet = codeSetApi.list(importedChildFolderId)
        then:
        importedChildCodeSet
        importedChildCodeSet.items.size() == 1
        importedChildCodeSet.items[0].id != childCodeSetId

        when:
        ListResponse<Annotation> importedAnnotationResponse = annotationApi.list("folder", importedFolderId)

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
        ListResponse<CodeSet> importedCodeSetResponse = codeSetApi.list(importedFolderId)

        then:
        importedCodeSetResponse
        importedCodeSetResponse.items.size() == 1
        importedCodeSetResponse.items[0].id != codeSet.id

        when:
        ListResponse<Terminology> importedTerminologyResponse = terminologyApi.list(importedFolderId)

        then:
        importedTerminologyResponse
        importedTerminologyResponse.items.size() == 1
        UUID importedTerminologyId = importedTerminologyResponse.items[0].id
        importedTerminologyId != terminology.id

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypeResponse =
                termRelationshipTypeApi.list(importedTerminologyId)

        then:
        importedTermRelationshipTypeResponse
        importedTermRelationshipTypeResponse.items.size() == 1
        UUID importedTermRelationshipTypeId = importedTermRelationshipTypeResponse.items[0].id
        importedTermRelationshipTypeId != termRelationshipType.id

        when:
        ListResponse<Term> importedTermsResponse = termApi.list(importedTerminologyId)

        then:
        importedTermsResponse
        importedTermsResponse.items.size() == 2

        importedTermsResponse.items.code.sort() == ['source code', 'target code']
        importedTermsResponse.items.id.sort() != [sourceTerm.id, targetTerm.id]

        when:
        ListResponse<TermRelationship> importedTermRelationship =
            termRelationshipApi.list(importedTerminologyId)
        then:
        importedTermRelationship
        importedTermRelationship.items.size() == 1
        importedTermRelationship.items[0].id != termRelationshipType.id
        importedTermRelationship.items[0].sourceTerm.code == 'source code'
        importedTermRelationship.items[0].targetTerm.code == 'target code'
        importedTermRelationship.items[0].relationshipType.id == importedTermRelationshipTypeId
    }


    void 'test consume export folder- folder is not parent - should import'() {
        given:

        UUID childFolderId = folderApi.create(folderId, new Folder(label: 'child folder')).id

        UUID childCodeSetId = codeSetApi.create(childFolderId, new CodeSet(label: 'codeset in child folder')).id

        Annotation annotation = annotationApi.create("folder", folderId, annotationPayload())
        Annotation childAnnotation =
            annotationApi.create("folder", folderId, annotation.id, annotationPayload('childLabel', 'child-description'))

        CodeSet codeSet = codeSetApi.create(folderId, codeSet())

        Terminology terminology = terminologyApi.create(folderId, terminologyPayload())

        TermRelationshipType termRelationshipType = termRelationshipTypeApi.create(terminology.id, termRelationshipType())

        Term sourceTerm = termApi.create(terminology.id, new Term(code: 'source code', definition: 'source term'))

        Term targetTerm = termApi.create(terminology.id, new Term(code: 'target code', definition: 'target term'))

        termRelationshipApi.create(terminology.id,
                new TermRelationship(
                        relationshipType: new TermRelationshipType(id: termRelationshipType.id),
                        sourceTerm      : new Term(id: sourceTerm.id),
                        targetTerm      : new Term(id: targetTerm.id)))


        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')

        MultipartBody importRequest = MultipartBody.builder()
                .addPart('folderId', childFolderId.toString())
                .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, exportResponse.body())
                .build()

        when:
        ListResponse<Folder> response = folderApi.importModel(importRequest, 'org.maurodata.plugin.importer.json', 'JsonFolderImporterPlugin', '4.0.0')

        then:
        response
        UUID importedFolderId = response.items.first().id

        when:
        Folder importedFolder = folderApi.show(importedFolderId)

        then:
        importedFolder


        when:
        ListResponse<Folder> importedChildFolders = folderApi.list(importedFolderId)
        then:
        importedChildFolders
        importedChildFolders.items.size() == 1
        UUID importedChildFolderId = importedChildFolders.items[0].id

        when:
        ListResponse<CodeSet> importedChildCodeSet = codeSetApi.list(importedChildFolderId)
        then:
        importedChildCodeSet
        importedChildCodeSet.items.size() == 1
        importedChildCodeSet.items[0].id != childCodeSetId


        when:
        ListResponse<Annotation> importedAnnotationResponse = annotationApi.list("folder", importedFolderId)

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
        ListResponse<CodeSet> importedCodeSetResponse = codeSetApi.list(importedFolderId)

        then:
        importedCodeSetResponse
        importedCodeSetResponse.items.size() == 1
        importedCodeSetResponse.items[0].id != codeSet.id

        when:
        ListResponse<Terminology> importedTerminologyResponse = terminologyApi.list(importedFolderId)

        then:
        importedTerminologyResponse
        importedTerminologyResponse.items.size() == 1
        UUID importedTerminologyId = importedTerminologyResponse.items[0].id
        importedTerminologyId != terminology.id

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypeResponse =
                termRelationshipTypeApi.list(importedTerminologyId)

        then:
        importedTermRelationshipTypeResponse
        importedTermRelationshipTypeResponse.items.size() == 1
        UUID importedTermRelationshipTypeId = importedTermRelationshipTypeResponse.items[0].id
        importedTermRelationshipTypeId != termRelationshipType.id

        when:
        ListResponse<Term> importedTermsResponse = termApi.list(importedTerminologyId)

        then:
        importedTermsResponse
        importedTermsResponse.items.size() == 2

        importedTermsResponse.items.code.sort() == ['source code', 'target code']
        importedTermsResponse.items.id.sort() != [sourceTerm.id, targetTerm.id].sort()

        when:
        ListResponse<TermRelationship> importedTermRelationship = termRelationshipApi.list(importedTerminologyId)
        then:
        importedTermRelationship
        importedTermRelationship.items.size() == 1
        importedTermRelationship.items[0].id != termRelationshipType.id
        importedTermRelationship.items[0].sourceTerm.code == 'source code'
        importedTermRelationship.items[0].targetTerm.code == 'target code'
        importedTermRelationship.items[0].relationshipType.id == importedTermRelationshipTypeId
    }

    void 'export and import folder with two terminologies with overlapping codes'() {
        given:
        // create two terminologies each with different Terms with code TEST
        UUID folderId = folderApi.create(new Folder(label: 'Two terminologies folder')).id
        UUID terminology1Id = terminologyApi.create(folderId, new Terminology(label: 'First Terminology')).id
        UUID term1Id = termApi.create(terminology1Id, new Term(code: 'TEST', definition: 'first term')).id
        UUID terminology2Id = terminologyApi.create(folderId, new Terminology(label: 'Second Terminology')).id
        UUID term2Id = termApi.create(terminology2Id, new Term(code: 'TEST', definition: 'second term')).id

        // also create two different term relationship types with label TEST
        UUID termRelationshipType1Id = termRelationshipTypeApi.create(
            terminology1Id, new TermRelationshipType(label: 'TEST', childRelationship: true)).id
        UUID termRelationshipType2Id = termRelationshipTypeApi.create(
            terminology2Id, new TermRelationshipType(label: 'TEST', childRelationship: false)).id
        termRelationshipApi.create(terminology1Id, new TermRelationship(
            relationshipType: new TermRelationshipType(id: termRelationshipType1Id),
            sourceTerm: new Term(id: term1Id),
            targetTerm: new Term(id: term1Id)
        ))
        termRelationshipApi.create(terminology2Id, new TermRelationship(
            relationshipType: new TermRelationshipType(id: termRelationshipType2Id),
            sourceTerm: new Term(id: term2Id),
            targetTerm: new Term(id: term2Id)
        ))

        when:
        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')
        Map export = jsonSlurper.parseText(new String(exportResponse.body())) as Map

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
        ListResponse<Folder> response = folderApi.importModel(importRequest, 'org.maurodata.plugin.importer.json', 'JsonFolderImporterPlugin', '4.0.0')
        UUID importedFolderId = response.items.first().id

        then:
        response.count == 1
        response.items.size() == 1
        response.items.first().label == 'Two terminologies folder'
        importedFolderId

        when:
        ListResponse<Terminology> importedTerminologies = terminologyApi.list(importedFolderId)
        UUID importedTerminology1Id = importedTerminologies.items.find {it.label == 'First Terminology'}.id
        UUID importedTerminology2Id = importedTerminologies.items.find {it.label == 'Second Terminology'}.id

        then:
        importedTerminologies.count == 2
        importedTerminologies.items.find {it.label == 'First Terminology'}
        importedTerminologies.items.find {it.label == 'Second Terminology'}

        when:
        ListResponse<Term> importedTerms = termApi.list(importedTerminology1Id)

        then:
        importedTerms.count == 1
        importedTerms.items.first().code == 'TEST'
        importedTerms.items.first().definition == 'first term'

        when:
        importedTerms = termApi.list(importedTerminology2Id)

        then:
        importedTerms.count == 1
        importedTerms.items.first().code == 'TEST'
        importedTerms.items.first().definition == 'second term'

        when:
        ListResponse<TermRelationshipType> importedTermRelationshipTypes =
            termRelationshipTypeApi.list(importedTerminology1Id)

        then:
        importedTermRelationshipTypes.count == 1
        importedTermRelationshipTypes.items.first().label == 'TEST'
        importedTermRelationshipTypes.items.first().childRelationship == true

        when:
        importedTermRelationshipTypes = termRelationshipTypeApi.list(importedTerminology2Id)

        then:
        importedTermRelationshipTypes.count == 1
        importedTermRelationshipTypes.items.first().label == 'TEST'
        importedTermRelationshipTypes.items.first().childRelationship == false

        when:
        ListResponse<TermRelationship> importedTermRelationships = termRelationshipApi.list(importedTerminology1Id)

        then:
        importedTermRelationships.count == 1
        importedTermRelationships.items.first().sourceTerm.code == 'TEST'
        importedTermRelationships.items.first().targetTerm.code == 'TEST'
        importedTermRelationships.items.first().relationshipType.label == 'TEST'

        when:
        importedTermRelationships = termRelationshipApi.list(importedTerminology2Id)

        then:
        importedTermRelationships.count == 1
        importedTermRelationships.items.first().sourceTerm.code == 'TEST'
        importedTermRelationships.items.first().targetTerm.code == 'TEST'
        importedTermRelationships.items.first().relationshipType.label == 'TEST'
    }


    void 'test export folder -datamodels with dataflow'(){
        given:
        dataClass1Id = dataClassApi.create(dataModelId, new DataClass(label: 'TEST-1', description: 'first data class')).id
        dataClass2Id = dataClassApi.create(dataModelId, new DataClass(label: 'TEST-2', description: 'second data class')).id
        dataType = dataTypeApi.create(dataModelId, new DataType(label: 'Test data type', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        dataElementId1 = dataElementApi.create(dataModelId, dataClass1Id, dataElementPayload('dataElement1 label', dataType)).id
        dataElementId2 = dataElementApi.create(dataModelId, dataClass2Id, dataElementPayload('label2', dataType)).id

        source = dataModelApi.create(folderId, dataModelPayload('source label'))
        dataFlowId = dataFlowApi.create(dataModelId, new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: source)).id

        dataClassComponentId = dataClassComponentApi.create(source.id, dataFlowId,
                                                            new DataClassComponent(
                                                                label: 'data class component test label')).id
        dataClassComponentApi.updateSource(source.id, dataFlowId, dataClassComponentId, dataClass1Id)
        dataClassComponentApi.updateTarget(source.id, dataFlowId, dataClassComponentId, dataClass2Id)
        dataElementComponentId =
            dataElementComponentApi.create(source.id, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(source.id, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId1)
        dataElementComponentApi.updateTarget(source.id, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId2)


        when:
        HttpResponse<byte[]> exportResponse = folderApi.exportModel(folderId, 'org.maurodata.plugin.exporter.json', 'JsonFolderExporterPlugin', '4.0.0')

        then:
        exportResponse.body()
        Map parsedJson = jsonSlurper.parseText(new String(exportResponse.body())) as Map
        parsedJson.folder.dataModels.size() == 2
        parsedJson.folder.dataModels[0].targetDataFlows.size() == 1
        parsedJson.folder.dataModels[0].targetDataFlows[0].dataClassComponents.size() == 1
        parsedJson.folder.dataModels[0].targetDataFlows[0].dataClassComponents[0].dataElementComponents.size() == 1
        parsedJson.folder.dataModels[0].targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].sourceDataElements
        parsedJson.folder.dataModels[0].targetDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements
        !parsedJson.folder.dataModels[0].sourceDataFlows

        !parsedJson.folder.dataModels[1].targetDataFlows
        parsedJson.folder.dataModels[1].sourceDataFlows.size() == 1
        parsedJson.folder.dataModels[1].sourceDataFlows[0].dataClassComponents.size() == 1
        parsedJson.folder.dataModels[1].sourceDataFlows[0].dataClassComponents[0].dataElementComponents.size() == 1
        parsedJson.folder.dataModels[1].sourceDataFlows[0].dataClassComponents[0].dataElementComponents[0].sourceDataElements.size() == 1
        parsedJson.folder.dataModels[1].sourceDataFlows[0].dataClassComponents[0].dataElementComponents[0].targetDataElements.size() == 1
    }
}
