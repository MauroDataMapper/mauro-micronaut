package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

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
        Folder folderResponse = (Folder) POST('/folders', [label: 'Test folder'], Folder)
        folderId = folderResponse.id

        when:
        Terminology terminologyResponse = (Terminology) POST("/folders/$folderId/terminologies", [label: 'Test terminology'], Terminology)
        terminologyId = terminologyResponse.id

        then:
        terminologyResponse
        terminologyResponse.label == 'Test terminology'
        terminologyResponse.path.toString() == 'te:Test terminology$main'
    }

    void 'test terms'() {
        when:
        Term termResponse = (Term) POST("/terminologies/$terminologyId/terms", [code: 'TEST-1', definition: 'first term'], Term)
        termId1 = termResponse.id

        then:
        termResponse.label == 'TEST-1: first term'

        when:
        termResponse = (Term) POST("/terminologies/$terminologyId/terms", [code: 'TEST-2', definition: 'second term'], Term)
        termId2 = termResponse.id

        then:
        termResponse.label == 'TEST-2: second term'

        when:
        ListResponse<Term> termListResponse = (ListResponse<Term>) GET("/terminologies/$terminologyId/terms", ListResponse<Term>)

        then:
        termListResponse
        termListResponse.count == 2
        termListResponse.items.path.sort().collect() {it.toString()} == ['te:Test terminology$main|tm:TEST-1', 'te:Test terminology$main|tm:TEST-2']
    }

    void 'test term relationship types'() {
        when:
        TermRelationshipType termRelationshipTypeResponse = (TermRelationshipType) POST("/terminologies/$terminologyId/termRelationshipTypes", [label: 'Test relationship type', childRelationship: true], TermRelationshipType)
        termRelationshipTypeId = termRelationshipTypeResponse.id

        then:
        termRelationshipTypeResponse
        termRelationshipTypeResponse.label == 'Test relationship type'
        termRelationshipTypeResponse.path.toString() == 'te:Test terminology$main|trt:Test relationship type'
        termRelationshipTypeResponse.childRelationship
        !termRelationshipTypeResponse.parentalRelationship
    }

    void 'test term relationships'() {
        when:
        TermRelationship termRelationshipResponse = (TermRelationship) POST("/terminologies/$terminologyId/termRelationships", [
            relationshipType: [id: termRelationshipTypeId],
            sourceTerm: [id: termId1],
            targetTerm: [id: termId2]
        ], TermRelationship)

        then:
        termRelationshipResponse
        termRelationshipResponse.label == 'Test relationship type'

        when:
        def tree = GET("/terminologies/$terminologyId/terms/tree", List<Map<String, Object>>)

        then:
        tree
        tree.size() == 1
        tree.first().code == 'TEST-2'

        when:
        List<TreeItem> treeItemList = (List<TreeItem>) GET("/terminologies/$terminologyId/terms/tree", List<TreeItem>)

        then:
        treeItemList
        treeItemList.size() == 1
        treeItemList.first().code == 'TEST-2'

        when:
        treeItemList = (List<TreeItem>) GET("/terminologies/$terminologyId/terms/tree/$termId2", List<TreeItem>)

        then:
        treeItemList != null
        treeItemList.size() == 1

        when:
        treeItemList = (List<TreeItem>) GET("/terminologies/$terminologyId/terms/tree/$termId1", List<TreeItem>)

        then:
        treeItemList != null
        treeItemList.size() == 0

    }
}
