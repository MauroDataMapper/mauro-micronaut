package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientException
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-rule.sql", phase = Sql.Phase.AFTER_EACH)
class RuleIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    UUID ruleId

    Map<String, String> ruleMap

    void setup() {
        Folder folder = folderApi.create(new Folder(label: 'Folder with Rules'))
        folderId = folder.id
    }

    void 'list empty Rules'() {
        when:
        def response =
                ruleApi.list('folder', folderId)

        then:
        response.count == 0
    }

    void 'create rule'() {

        when:
        Rule rule = ruleApi.create('folder', folderId, rulePayload())

        then:
        rule
        rule.id != null
        rule.domainType == "Rule"
        rule.name == rulePayload().name
    }

    void 'list rules'() {
        when:
        ruleApi.create('folder', folderId, rulePayload())
        ListResponse<Rule> response = ruleApi.list('folder', folderId)

        then:
        response
        response.count == 1
        response.items.first().name == 'rule name'
    }

    void 'get rule by Id'() {
        Rule rule = ruleApi.create('folder', folderId, rulePayload())
        ruleId = rule.id

        when:
        Rule saved = ruleApi.show('folder', folderId, ruleId)

        then:
        saved
        saved.id == ruleId
        saved.name == 'rule name'
    }

    void 'update rule'() {
        when:
        Rule rule = ruleApi.create('folder', folderId, rulePayload())
        ruleId = rule.id

        Rule updated = ruleApi.update('folder', folderId, ruleId, new Rule(name: 'new rule name'))

        then:
        updated
        updated.name == 'new rule name'

        when:
        Rule retrieved = ruleApi.show('folder', folderId, ruleId)

        then:
        retrieved
        retrieved.name == 'new rule name'
        retrieved.id == rule.id
    }

    void 'delete rule'() {
        when:
        Rule rule = ruleApi.create('folder', folderId, rulePayload())
        ruleId = rule.id

        HttpResponse response = ruleApi.delete('folder', folderId, ruleId)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        Rule ruleResponse = ruleApi.show('folder', folderId, ruleId)

        then: 'the show endpoint shows the update'
        !ruleResponse

        when:
        ListResponse<Rule> rulesResponse = ruleApi.list('folder', folderId)

        then: 'the list endpoint shows the update'
        rulesResponse
        rulesResponse.count == 0
    }

    void 'delete report'() {
        given:
        Rule rule = ruleApi.create('folder', folderId, rulePayload())

        and:
        RuleRepresentation ruleRepresentation1 = ruleRepresentationApi.create(
            'folder',
            folderId,
            rule.id,
            new RuleRepresentation(language: 'language-1'))
        RuleRepresentation ruleRepresentation2 = ruleRepresentationApi.create(
            'folder',
            folderId,
            rule.id,
            new RuleRepresentation(language: 'language-2'))

        ListResponse<RuleRepresentation> savedReports = ruleRepresentationApi.list('folder', folderId, rule.id)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(ruleRepresentation1.id, ruleRepresentation2.id)

        when:
        HttpResponse deleteResponse = ruleRepresentationApi.delete('folder', folderId, rule.id, ruleRepresentation1.id)

        then:
        deleteResponse.status == HttpStatus.NO_CONTENT

        when:
        Rule ruleResponse = ruleApi.show('folder', folderId, rule.id)

        then:
        ruleResponse.ruleRepresentations.size() == 1
        ruleResponse.ruleRepresentations.id == [ruleRepresentation2.id]

        when:
        ruleResponse = ruleRepresentationApi.show('folder', folderId, rule.id, ruleRepresentation1.id)

        then: '404 not found is returned, exception thrown'
        !ruleResponse
    }


    void 'delete rule with reports'() {
        given:
        Rule rule = ruleApi.create('folder', folderId, rulePayload())

        and:
        RuleRepresentation ruleRepresentation1 = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                new RuleRepresentation(language: 'language-1'))
        RuleRepresentation ruleRepresentation2 = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                new RuleRepresentation(language: 'language-2'))

        ListResponse<RuleRepresentation> savedReports = ruleRepresentationApi.list('folder', folderId, rule.id)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(ruleRepresentation1.id, ruleRepresentation2.id)

        when:
        HttpResponse deleteResponse = ruleApi.delete('folder', folderId, rule.id)

        then:
        deleteResponse.status == HttpStatus.NO_CONTENT

        when:
        Rule ruleResponse = ruleApi.show('folder', folderId, rule.id)

        then: '404 not found is returned, exception thrown'
        !ruleResponse

        when:
        List<RuleRepresentation> representations = ruleRepresentationApi.list('folder', folderId, rule.id)

        then: '404 not found is returned, exception thrown'
        !representations

        when:
        ListResponse<Rule> response = ruleApi.list('folder', folderId)

        then: 'no rules are found'
        response
        response.count == 0

    }
}
