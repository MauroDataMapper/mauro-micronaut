package uk.ac.ox.softeng.mauro.facet

import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
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

import java.time.Instant

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-rule.sql", phase = Sql.Phase.AFTER_EACH)
class RuleRepresentationIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    @Shared
    Rule rule

    void setup() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with Rules'], Folder)
        folderId = folder.id
        rule = (Rule) POST("$FOLDERS_PATH/$folderId$RULE_PATH", rulePayload(), Rule)
    }

    void 'list empty Rule Representations'() {
        when:
        def response =
                GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH", ListResponse, RuleRepresentation)
        then:
        response.count == 0
    }

    void 'create Rule Representation'() {
        when:
        RuleRepresentation ruleRepresentation = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                                                                          ruleRepresentation(), RuleRepresentation)

        then:
        ruleRepresentation
        ruleRepresentation.id != null
        ruleRepresentation.domainType == "RuleRepresentation"
        ruleRepresentation.ruleId == rule.id
        ruleRepresentation.language == 'java'
    }

    void 'list Rule Representations'() {
        given:
        RuleRepresentation ruleRepresentation1 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                ruleRepresentation(), RuleRepresentation)
        RuleRepresentation ruleRepresentation2 = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
               ruleRepresentation(), RuleRepresentation)
        when:
        ListResponse<RuleRepresentation> response = (ListResponse<RuleRepresentation>) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH", ListResponse, RuleRepresentation)

        then:
        response
        response.count == 2
        response.items.id.collect() { it.toString() } == ["$ruleRepresentation1.id", "$ruleRepresentation2.id"] as List<String>
        response.items.ruleId.collect().unique { it.toString() }.size() == 1
        response.items.ruleId.collect().unique { it.toString() }[0].toString() == "$rule.id"
        response.items.language
    }

    void 'get rule representation by Id'() {
        given:
        RuleRepresentation report = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
           ruleRepresentation(), RuleRepresentation)

        when:
        RuleRepresentation retrieved = GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH/$report.id",
                                           RuleRepresentation)

        then:
        retrieved
        retrieved.id == report.id
        retrieved.ruleId == rule.id
        retrieved.language == 'java'
    }

    void 'update rule representation by Id'() {
        given:
        RuleRepresentation report = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                [language: 'java'], RuleRepresentation)

        and:
        def dataAsMap =   [language: 'c++']

        when:
        RuleRepresentation updated = (RuleRepresentation) PUT("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH/$report.id",
                dataAsMap, RuleRepresentation)
        then:
        updated
        updated.language ==  dataAsMap.get('language')
    }

    void 'delete RuleRepresentation'() {
        given:
        RuleRepresentation report = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                [language: 'java'], RuleRepresentation)

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH/$report.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT
        when:
        Rule retrieved = (Rule) GET("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id", Rule)

        then: 'Associated summary metadata is not affected'
        retrieved
        retrieved.id == rule.id
        retrieved.domainType == rule.domainType
    }

    void 'delete non existing Rule Representation - should throw exception with http status not found'() {
        when:
        (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$SUMMARY_METADATA_REPORT_PATH/$rule.id", HttpStatus)

        then: 'not found exception should be thrown'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

    }

    void 'list rule - contains ruleRepresentation'() {
        given:
        RuleRepresentation rep = (RuleRepresentation) POST("$FOLDERS_PATH/$folderId$RULE_PATH/$rule.id$RULE_REPRESENTATION_PATH",
                ruleRepresentation(), RuleRepresentation)
        when:
        ListResponse<Rule> ruleResponse = (ListResponse<Rule>) GET("$FOLDERS_PATH/$folderId$RULE_PATH", ListResponse, Rule)

        then:
        ruleResponse
        ruleResponse.count == 1
        ruleResponse.items[0].ruleRepresentations.size() == 1
        ruleResponse.items[0].ruleRepresentations[0].id == rep.id
    }
}
