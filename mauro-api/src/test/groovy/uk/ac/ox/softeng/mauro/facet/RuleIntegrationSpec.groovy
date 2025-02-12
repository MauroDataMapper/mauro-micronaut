package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.client.exceptions.HttpClientException
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
@Sql(scripts = "classpath:sql/tear-down-rule.sql", phase = Sql.Phase.AFTER_EACH)
class RuleIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    UUID ruleId

    Map<String, String> ruleMap

    void setup() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with Rules'], Folder)
        folderId = folder.id
    }

    void 'list empty Rules'() {
        when:
        def response =
                GET("$FOLDERS_PATH/$folderId$RULE_PATH", ListResponse, Rule)

        then:
        response.count == 0
    }

    void 'create rule'() {
        given:
        ruleMap = rulePayload()

        when:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                                           ruleMap, Rule)

        then:
        rule
        rule.id != null
        rule.domainType == "Rule"
        rule.name == ruleMap.name
    }

    void 'list rules'() {
        given:
        ruleMap = rulePayload()
        and:
        (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH", ruleMap, Rule)
        when:
        ListResponse<Rule> response = (ListResponse<Rule>) GET("$FOLDERS_PATH/$folderId$RULE_PATH", ListResponse, Rule)

        then:
        response
        response.count == 1
        response.items.first().name == 'rule name'
    }

    void 'get rule by Id'() {
        given:
        ruleMap = rulePayload()

        and:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                ruleMap, Rule)
        ruleId = rule.id

        when:
        Rule saved = GET("$FOLDERS_PATH/$folderId$RULE_PATH/$ruleId",
                Rule)

        then:
        saved
        saved.id == ruleId
        saved.name == 'rule name'
    }

    void 'update rule'() {
        given:
        ruleMap = rulePayload()

        and:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                ruleMap, Rule)
        ruleId = rule.id

        when:
        Rule updated = (Rule) PUT("$FOLDERS_PATH/$folderId$RULE_PATH/$ruleId",
                [name: 'new rule name'], Rule)

        then:
        updated
        updated.name == 'new rule name'

        when:
        Rule retrieved = (Rule) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$ruleId", Rule)

        then:
        retrieved
        retrieved.name == 'new rule name'
        retrieved.id == rule.id
    }

    void 'delete rule'() {
        given:
        ruleMap = rulePayload()
        and:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                ruleMap, Rule)
        ruleId = rule.id

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$RULE_PATH/$ruleId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (Rule) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$ruleId", Rule)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        ListResponse<Rule> response = (ListResponse<Rule>) GET("$FOLDERS_PATH/$folderId$RULE_PATH", ListResponse, Rule)

        then: 'the list endpoint shows the update'
        response
        response.count == 0
    }

    void 'delete report'() {
        given:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                rulePayload(), Rule)

        and:
        RuleRepresentation ruleRepresentation1 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                [language: 'language-1'], RuleRepresentation)
        RuleRepresentation ruleRepresentation2 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                [language: 'language-2'], RuleRepresentation)

        ListResponse<RuleRepresentation> savedReports = (ListResponse<RuleRepresentation>) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                ListResponse, RuleRepresentation)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(ruleRepresentation1.id, ruleRepresentation2.id)

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$RULE_PATH/${rule.id}/$RULE_REPRESENTATION_PATH/${ruleRepresentation1.id}", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        Rule ruleResponse = (Rule) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id", Rule)

        then:
        ruleResponse.ruleRepresentations.size() == 1
        ruleResponse.ruleRepresentations.id == [ruleRepresentation2.id]

        when:
        (RuleRepresentation) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH/${ruleRepresentation1.id}", RuleRepresentation)

        then: '404 not found is returned, exception thrown'
        HttpClientException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

    }


    void 'delete rule with reports'() {
        given:
        Rule rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH",
                rulePayload(), Rule)

        and:
        RuleRepresentation ruleRepresentation1 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                                                                           [language: 'language-1'], RuleRepresentation)
        RuleRepresentation ruleRepresentation2 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                                                                           [language: 'language-2'], RuleRepresentation)

        ListResponse<RuleRepresentation> savedReports = (ListResponse<RuleRepresentation>) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                ListResponse, RuleRepresentation)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(ruleRepresentation1.id, ruleRepresentation2.id)

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (Rule) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id", Rule)

        then: '404 not found is returned, exception thrown'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        (ListResponse<RuleRepresentation>) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                ListResponse, RuleRepresentation)

        then: '404 not found is returned, exception thrown'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        ListResponse<Rule> response = (ListResponse<Rule>) GET("$FOLDERS_PATH/$folderId$RULE_PATH", ListResponse, Rule)

        then: 'no rules are found'
        response
        response.count == 0

    }
}
