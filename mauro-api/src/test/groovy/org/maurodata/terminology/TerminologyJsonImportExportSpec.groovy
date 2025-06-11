package org.maurodata.terminology

import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
class TerminologyJsonImportExportSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    ExportModel exportModel

    void 'create terminology and export'() {
        given:
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        UUID terminologyId = terminologyApi.create(folderId, new Terminology(label: 'Test terminology')).id
        UUID term1Id = termApi.create(terminologyId, new Term(code: 'TEST-1', definition: 'first term')).id
        UUID term2Id = termApi.create(terminologyId, new Term(code: 'TEST-2', definition: 'second term')).id
        UUID termRelationshipTypeId = termRelationshipTypeApi.create(
            terminologyId, new TermRelationshipType(label: 'Test relationship type', childRelationship: true)).id
        termRelationshipApi.create(terminologyId, new TermRelationship(
            relationshipType: new TermRelationshipType(id: termRelationshipTypeId),
            sourceTerm: new Term(id: term1Id),
            targetTerm: new Term(id: term2Id)))

        when:
        byte[] responseBytes = terminologyApi.exportModel(
            terminologyId,
            'org.maurodata.plugin.exporter.json',
            'JsonTerminologyExporterPlugin',
            '4.0.0').body()

        exportModel = objectMapper.readValue(responseBytes, ExportModel)
        then:
        exportModel.terminology.label == 'Test terminology'
        exportModel.terminology.terms.code == ['TEST-1','TEST-2']
    }

    void 'import terminology and verify'() {
        given:
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()

        def request = terminologyApi.importModel(
            importRequest,
            'org.maurodata.plugin.importer.json',
            'JsonTerminologyImporterPlugin',
            '4.0.0')

        when:
        UUID importedTerminologyId = request.items.first().id
        Terminology terminology = terminologyApi.show(importedTerminologyId)

        then:
        terminology.path.toString() == 'fo:Test folder|te:Test terminology$main'

        when:
        ListResponse<Term> terms = termApi.list(importedTerminologyId)

        then:
        terms.items.path.collect { it.toString()}.sort() == ['fo:Test folder|te:Test terminology$main|tm:TEST-1', 'fo:Test folder|te:Test terminology$main|tm:TEST-2']

        when:
        ListResponse<TermRelationshipType> termRelationshipTypes =
            termRelationshipTypeApi.list(importedTerminologyId)

        then:
        termRelationshipTypes.items.path.collect { it.toString()}
            == ['fo:Test folder|te:Test terminology$main|trt:Test relationship type']

        when:
        List<Term> tree = termApi.tree(importedTerminologyId, null)

        then:
        tree.code == ['TEST-2']
    }
}
