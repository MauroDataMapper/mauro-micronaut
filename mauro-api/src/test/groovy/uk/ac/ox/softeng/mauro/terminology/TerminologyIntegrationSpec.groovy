package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import io.micronaut.core.type.Argument
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared

@MicronautTest
class TerminologyIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId

    @Shared
    UUID termId1

    @Shared
    UUID termId2

    @Shared
    UUID termRelationshipTypeId

    void 'test terminology'() {
        given:
        def response = POST('/folders', [label: 'Test folder'])
        folderId = UUID.fromString(response.id)

        when:
        response = POST("/folders/$folderId/terminologies", [label: 'Test terminology'])
        terminologyId = UUID.fromString(response.id)

        then:
        response
        response.label == 'Test terminology'
        response.path == 'te:Test terminology$main'
    }

    void 'test terms'() {
        when:

        def response = POST("/terminologies/$terminologyId/terms", [code: 'TEST-1', definition: 'first term'])
        termId1 = UUID.fromString(response.id)

        then:
        response.label == 'TEST-1: first term'

        when:
        response = POST("/terminologies/$terminologyId/terms", [code: 'TEST-2', definition: 'second term'])
        termId2 = UUID.fromString(response.id)

        then:
        response.label == 'TEST-2: second term'

        when:
        response = GET("/terminologies/$terminologyId/terms")

        then:
        response
        response.count == 2
        response.items.path.sort() == ['te:Test terminology$main|tm:TEST-1', 'te:Test terminology$main|tm:TEST-2']
    }

    void 'test term relationship types'() {
        when:
        def response = POST("/terminologies/$terminologyId/termRelationshipTypes", [label: 'Test relationship type', childRelationship: true])
        termRelationshipTypeId = UUID.fromString(response.id)

        then:
        response
        response.label == 'Test relationship type'
        response.path == 'te:Test terminology$main|trt:Test relationship type'
        response.childRelationship
        !response.parentalRelationship
    }

    void 'test term relationships'() {
        when:
        def response = POST("/terminologies/$terminologyId/termRelationships", [
            relationshipType: [id: termRelationshipTypeId],
            sourceTerm: [id: termId1],
            targetTerm: [id: termId2]
        ])

        then:
        response
        response.label == 'Test relationship type'

        when:
        def tree = GET("/terminologies/$terminologyId/terms/tree", List<Map<String, Object>>)

        then:
        tree
        tree.size() == 1
        tree.first().code == 'TEST-2'

        when:
        tree = GET("/terminologies/$terminologyId/terms/tree/$termId2", List<Map<String, Object>>)

        then:
        tree
        tree.size() == 1
        tree.first().code == 'TEST-1'

        when:
        tree = GET("/terminologies/$terminologyId/terms/tree/$termId1", List<Map<String, Object>>)

        then:
        tree != null
        tree.size() == 0
    }
}
