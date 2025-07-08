package org.maurodata.terminology


import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.tree.TreeItem
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
class TerminologyIntegrationSpec extends CommonDataSpec {

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
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        folderId = folderResponse.id

        when:
        Terminology terminologyResponse = terminologyApi.create(folderId, new Terminology(label: 'Test terminology'))
        terminologyId = terminologyResponse.id

        then:
        terminologyResponse
        terminologyResponse.label == 'Test terminology'
        terminologyResponse.path.toString() == 'fo:Test folder|te:Test terminology$main'
        terminologyResponse.authority
    }

    void 'test terms'() {
        when:
        Term termResponse = termApi.create(terminologyId, new Term(code: 'TEST-1', definition: 'first term'))
        termId1 = termResponse.id

        then:
        termResponse.label == 'TEST-1: first term'

        when:
        termResponse = termApi.create(terminologyId, new Term(code: 'TEST-2', definition: 'second term'))
        termId2 = termResponse.id

        then:
        termResponse.label == 'TEST-2: second term'

        when:
        ListResponse<Term> termListResponse = termApi.list(terminologyId)

        then:
        termListResponse
        termListResponse.count == 2
        termListResponse.items.path.collect{ it.toString() }.sort() == ['fo:Test folder|te:Test terminology$main|tm:TEST-1', 'fo:Test folder|te:Test terminology$main|tm:TEST-2']
    }

    void 'test term relationship types'() {
        when:
        TermRelationshipType termRelationshipTypeResponse = termRelationshipTypeApi.create(terminologyId,
            new TermRelationshipType(label: 'Test relationship type', childRelationship: true))
        termRelationshipTypeId = termRelationshipTypeResponse.id

        then:
        termRelationshipTypeResponse
        termRelationshipTypeResponse.label == 'Test relationship type'
        termRelationshipTypeResponse.displayLabel == 'Test Relationship Type'
        termRelationshipTypeResponse.path.toString() == 'fo:Test folder|te:Test terminology$main|trt:Test relationship type'
        termRelationshipTypeResponse.childRelationship
        !termRelationshipTypeResponse.parentalRelationship
    }

    void 'test term relationships'() {
        when:
        TermRelationship termRelationshipResponse =
            termRelationshipApi.create(terminologyId, new TermRelationship(
            relationshipType: new TermRelationshipType(id: termRelationshipTypeId),
            sourceTerm: new Term(id: termId1),
            targetTerm: new Term(id: termId2)
        ))

        then:
        termRelationshipResponse
        termRelationshipResponse.label == 'Test relationship type'

        when:
        List<TreeItem> treeItemList = termApi.tree(terminologyId, null)

        then:
        treeItemList
        treeItemList.size() == 1
        treeItemList.first().code == 'TEST-2'

        when:
        treeItemList = termApi.tree(terminologyId, termId2)

        then:
        treeItemList
        treeItemList.size() == 1

        treeItemList.first().code == 'TEST-1'

        when:
        treeItemList = termApi.tree(terminologyId, termId1)

        then:
        treeItemList != null
        treeItemList.size() == 0

    }

    void "test search within model"() {

        expect:

        ListResponse<SearchResultsDTO> searchResults = terminologyApi.searchGet(
            terminologyId, new SearchRequestDTO(searchTerm: searchTerm, domainTypes: domainTypes))
        searchResults.items.label == expectedLabels

        where:
        searchTerm          | domainTypes                               | expectedLabels
        "first"             | []                                        | ["TEST-1: first term"]
        "second"            | []                                        | ["TEST-2: second term"]
        "term"              | []                                        | ["TEST-1: first term", "TEST-2: second term"]
        "term"              | ["Term"]                                  | ["TEST-1: first term", "TEST-2: second term"]
        "term"              | ["TermRelationship"]                      | []
        "test"              | []                                        | ["Test terminology", "TEST-1: first term", "TEST-2: second term"]
        "test"              | ["Terminology"]                           | ["Test terminology"]

    }

}
