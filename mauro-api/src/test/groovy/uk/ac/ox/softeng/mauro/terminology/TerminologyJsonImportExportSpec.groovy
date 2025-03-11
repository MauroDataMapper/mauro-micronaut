package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
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
            'uk.ac.ox.softeng.mauro.plugin.exporter.json',
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
            'uk.ac.ox.softeng.mauro.plugin.importer.json',
            'JsonTerminologyImporterPlugin',
            '4.0.0')

        when:
        UUID importedTerminologyId = request.items.first().id
        Terminology terminology = terminologyApi.show(importedTerminologyId)

        then:
        terminology.path.toString() == 'te:Test terminology$main'

        when:
        ListResponse<Term> terms = termApi.list(importedTerminologyId)

        then:
        terms.items.path.collect { it.toString()}.sort() == ['te:Test terminology$main|tm:TEST-1', 'te:Test terminology$main|tm:TEST-2']

        when:
        ListResponse<TermRelationshipType> termRelationshipTypes =
            termRelationshipTypeApi.list(importedTerminologyId)

        then:
        termRelationshipTypes.items.path.collect { it.toString()}
            == ['te:Test terminology$main|trt:Test relationship type']

        when:
        List<Term> tree = termApi.tree(importedTerminologyId, null)

        then:
        tree.code == ['TEST-2']
    }
}
