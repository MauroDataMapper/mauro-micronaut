package org.maurodata.terminology

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
        termId2 = termApi.create(terminologyId, new Term(description: 'Target Term description',
                                                         code: 'term-code ',
                                                         definition: 'some definition')).id
    }

    void 'test termRelationshipByTermList'() {
        given:
        Term source = termApi.show(terminologyId, termId1)
        Term target = termApi.show(terminologyId, termId2)
        termRelationshipApi.create(terminologyId, new TermRelationship(
            relationshipType: termRelationshipTypeApi.create(
                terminologyId, new TermRelationshipType(label: 'TEST', childRelationship: true)),
            sourceTerm: source,
            targetTerm: target)
        )
        when:
        ListResponse<TermRelationship> termRelationshipListResponse = termRelationshipApi.byTerminologyAndTermIdList(terminologyId, termId1)

        then:
        termRelationshipListResponse
        termRelationshipListResponse.items.size() == 1
        termRelationshipListResponse.items.first().sourceTerm.id == termId1

        when:
        termRelationshipListResponse = termRelationshipApi.byTerminologyAndTermIdList(terminologyId, termId2)
        then:
        termRelationshipListResponse
        termRelationshipListResponse.items.size() == 1
        termRelationshipListResponse.items.first().targetTerm.id == termId2

    }

}
