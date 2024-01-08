package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest
class FolderIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID childFolderId

    void 'create folder'() {

        // TODO

        when:
        def response = POST('/folders', [label: 'Test folder'])
        folderId = UUID.fromString(response.id)

        then:
        response
        response.label == 'Test folder'
        response.path == 'fo:Test folder'
    }

    void 'create child folder'() {
        when:
        def response = POST("/folders/$folderId/folders", [label: 'Test child folder'])
        childFolderId = UUID.fromString(response.id)

        then:
        response
        response.label == 'Test child folder'
        response.path == 'fo:Test folder|fo:Test child folder'
    }

    void 'change child folder label'() {
        when:
        def response = PUT("/folders/$folderId/folders/$childFolderId", [label: 'Updated child folder'])

        then:
        response
        response.label == 'Updated child folder'
        response.path == 'fo:Test folder|fo:Updated child folder'
    }

    void 'change parent folder label'() {
        when:
        def response = PUT("/folders/$folderId", [label: 'Updated folder'])

        then:
        response
        response.label == 'Updated folder'
        response.path == 'fo:Updated folder'
    }

    void 'list folders'() {
        when:
        def response = GET('/folders')

        then:
        response
        response.count == 2
        response.items.path.sort() == ['fo:Updated folder', 'fo:Updated folder|fo:Updated child folder']
    }
}
