package org.maurodata.facet

import io.micronaut.http.HttpResponse
import jakarta.inject.Singleton
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared

import java.time.Instant

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-rule.sql", phase = Sql.Phase.AFTER_EACH)
class RuleRepresentationIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    Rule rule

    void setup() {
        Folder folder = folderApi.create( new Folder(label: 'Folder with Rules'))
        folderId = folder.id
        rule = ruleApi.create('folder', folderId, rulePayload())
    }

    void 'list empty Rule Representations'() {
        when:
        def response = ruleRepresentationApi.list('folder', folderId, rule.id)
        then:
        response.count == 0
    }

    void 'create Rule Representation'() {
        when:
        RuleRepresentation ruleRepresentation = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                ruleRepresentation())

        then:
        ruleRepresentation
        ruleRepresentation.id != null
        ruleRepresentation.domainType == "RuleRepresentation"
        ruleRepresentation.ruleId == rule.id
        ruleRepresentation.language == 'java'
    }

    void 'list Rule Representations'() {
        given:
        RuleRepresentation ruleRepresentation1 = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                ruleRepresentation())
        RuleRepresentation ruleRepresentation2 = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
               ruleRepresentation())
        when:
        ListResponse<RuleRepresentation> response = ruleRepresentationApi.list('folder', folderId, rule.id)

        then:
        response
        response.count == 2
        response.items.id == [ruleRepresentation1.id, ruleRepresentation2.id]
        response.items.ruleId.collect().unique { it.toString() }.size() == 1
        response.items.ruleId.collect().unique { it.toString() }[0].toString() == "$rule.id"
        response.items.language
    }

    void 'get rule representation by Id'() {
        given:
        RuleRepresentation report = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                ruleRepresentation())

        when:
        RuleRepresentation retrieved = ruleRepresentationApi.show('folder', folderId, rule.id, report.id)

        then:
        retrieved
        retrieved.id == report.id
        retrieved.ruleId == rule.id
        retrieved.language == 'java'
    }

    void 'update rule representation by Id'() {
        given:
        RuleRepresentation report = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                new RuleRepresentation(language: 'java'))

        when:
        RuleRepresentation updated = ruleRepresentationApi.update(
                'folder',
                folderId,
                rule.id,
                report.id,
                new RuleRepresentation(language: 'c++'))
        then:
        updated
        updated.language == 'c++'
    }

    void 'delete RuleRepresentation'() {
        given:
        RuleRepresentation report = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                new RuleRepresentation(language: 'java'))

        when:
        HttpResponse deleteResponse = ruleRepresentationApi.delete(
                'folder',
                folderId,
                rule.id,
                report.id)

        then:
        deleteResponse.status == HttpStatus.NO_CONTENT

        when:
        Rule retrieved = ruleApi.show('folder',  folderId, rule.id)

        then: 'Associated summary metadata is not affected'
        retrieved
        retrieved.id == rule.id
        retrieved.domainType == rule.domainType
    }

    void 'delete non existing Rule Representation - should throw exception with http status not found'() {
        when:
        HttpResponse deleteResponse = ruleRepresentationApi.delete('folder', folderId, rule.id, rule.id)

        then: 'not found exception should be thrown'
        deleteResponse.status == HttpStatus.NOT_FOUND

    }

    void 'list rule - contains ruleRepresentation'() {
        given:
        RuleRepresentation rep = ruleRepresentationApi.create(
                'folder',
                folderId,
                rule.id,
                ruleRepresentation())
        when:
        ListResponse<Rule> ruleResponse = ruleApi.list('folder', folderId)

        then:
        ruleResponse
        ruleResponse.count == 1
        ruleResponse.items[0].ruleRepresentations.size() == 1
        ruleResponse.items[0].ruleRepresentations[0].id == rep.id
    }
}
