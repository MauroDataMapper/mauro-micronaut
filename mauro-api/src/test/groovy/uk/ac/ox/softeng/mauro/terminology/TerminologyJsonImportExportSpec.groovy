package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared

@ContainerizedTest
class TerminologyJsonImportExportSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    String exportJson

    void 'create terminology and export'() {
        given:
        folderId = UUID.fromString(POST('/folders', [label: 'Test folder']).id)
        UUID terminologyId = UUID.fromString(POST("/folders/$folderId/terminologies", [label: 'Test terminology']).id)
        UUID term1Id = UUID.fromString(POST("/terminologies/$terminologyId/terms", [code: 'TEST-1', definition: 'first term']).id)
        UUID term2Id = UUID.fromString(POST("/terminologies/$terminologyId/terms", [code: 'TEST-2', definition: 'second term']).id)
        UUID termRelationshipTypeId = UUID.fromString(POST("/terminologies/$terminologyId/termRelationshipTypes", [label: 'Test relationship type', childRelationship: true]).id)
        POST("/terminologies/$terminologyId/termRelationships", [
            relationshipType: [id: termRelationshipTypeId],
            sourceTerm: [id: term1Id],
            targetTerm: [id: term2Id]
        ])

        when:
        String response = GET("/terminologies/$terminologyId/export", String)
        exportJson = response

        then:
        response
    }

    void 'import terminology and verify'() {
        given:
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, exportJson.bytes)
            .build()
        def request = POST('/terminologies/import/uk.ac.ox.softeng.mauro.plugin.importer.json/JsonTerminologyImporterPlugin/4.0.0', importRequest)

        when:
        UUID importedTerminologyId = UUID.fromString(request.items.first().id)
        def terminology = GET("/terminologies/$importedTerminologyId")

        then:
        terminology.path == 'te:Test terminology$main'

        when:
        def terms = GET("/terminologies/$importedTerminologyId/terms")

        then:
        terms.items.path.sort() == ['te:Test terminology$main|tm:TEST-1', 'te:Test terminology$main|tm:TEST-2']

        when:
        def termRelationshipTypes = GET("/terminologies/$importedTerminologyId/termRelationshipTypes")

        then:
        termRelationshipTypes.items.path == ['te:Test terminology$main|trt:Test relationship type']

        when:
        def tree = GET("/terminologies/$importedTerminologyId/terms/tree", List<Map<String, Object>>)

        then:
        tree.code == ['TEST-2']
    }
}
