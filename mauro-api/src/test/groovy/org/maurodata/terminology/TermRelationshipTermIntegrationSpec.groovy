package org.maurodata.terminology

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class TermRelationshipTermIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId

    @Shared
    UUID termId1

    @Shared
    UUID termId2


    void setup() {
        folderId = folderApi.create(folder()).id
        terminologyId = terminologyApi.create(folderId, terminology()).id
        termId1 = termApi.create(terminologyId, term()).id
        termId2 = termApi.create(terminologyId, termPayload('target term code', 'target term descdription', 'targetTerm definition')).id
    }

    void 'test termRelationshipByTermList'() {
        given:
        Term source = termApi.show(terminologyId, termId1)
        Term target = termApi.show(terminologyId, termId2)
        Term other = termApi.create(terminologyId, termPayload('other code', 'other description', 'other definition'))
        TermRelationshipType termRelationshipType = termRelationshipTypeApi.create(
            terminologyId, termRelationshipTypePayload('Test', true))

        TermRelationship termRelationship = termRelationshipApi.create(terminologyId, new TermRelationship(relationshipType: termRelationshipType, sourceTerm: source, targetTerm: target)
        )
        TermRelationship termRelationshipOther = termRelationshipApi.createByTerminologyAndTerm(terminologyId, termId1,
                                                                                                termRelationshipPayload(termRelationshipType, other, other))
        when:
        ListResponse<TermRelationship> termRelationshipListResponse = termRelationshipApi.listByTerminologyAndTerm(terminologyId, termId1)

        then:
        termRelationshipListResponse
        termRelationshipListResponse.items.size() == 1
        termRelationshipListResponse.items.first().sourceTerm.id == termId1

        when:
        termRelationshipListResponse = termRelationshipApi.listByTerminologyAndTerm(terminologyId, termId2)
        then:
        termRelationshipListResponse
        termRelationshipListResponse.items.size() == 1
        termRelationshipListResponse.items.first().targetTerm.id == termId2

        when:
        termRelationshipListResponse = termRelationshipApi.list(terminologyId)
        then:
        termRelationshipListResponse
        termRelationshipListResponse.items.size() == 2
        termRelationshipListResponse.items.id.containsAll([termRelationshipOther.id, termRelationship.id])

    }

    void 'test termRelationshipByTermCreate, show delete'() {
        given:
        Term source = termApi.show(terminologyId, termId1)
        Term target = termApi.show(terminologyId, termId2)
        TermRelationshipType termRelationshipType = termRelationshipTypeApi.create(terminologyId, termRelationshipType())

        when:
        TermRelationship termRelationship = termRelationshipApi.createByTerminologyAndTerm(terminologyId, termId1,
                                                                                           termRelationshipPayload(termRelationshipType, source, target))

        then:
        termRelationship
        termRelationship.targetTerm == target
        termRelationship.sourceTerm == source
        termRelationship.relationshipType == termRelationshipType


        when:
        TermRelationship termRelationshipResponse = termRelationshipApi.showByTerminologyAndTerm(terminologyId, source.id, termRelationship.id)

        then:
        termRelationshipResponse
        termRelationshipResponse.id == termRelationship.id
        termRelationshipResponse == termRelationship

        when:
        HttpResponse deleteResponse = termRelationshipApi.delete(terminologyId, source.id, termRelationship.id,termRelationship )
        then:
        deleteResponse.status() == HttpStatus.NO_CONTENT
    }


}
