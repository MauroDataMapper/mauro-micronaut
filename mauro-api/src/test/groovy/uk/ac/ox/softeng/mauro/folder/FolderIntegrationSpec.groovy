package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

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
        when:
        Folder folderResponse = (Folder) POST('/folders', [label: 'Test folder'], Folder)
        folderId = folderResponse.id

        then:
        folderResponse
        folderResponse.label == 'Test folder'
        folderResponse.path.toString() == 'fo:Test folder'
    }

    void 'create child folder'() {
        when:
        Folder folderResponse = (Folder) POST("/folders/$folderId/folders", [label: 'Test child folder'], Folder)
        childFolderId = folderResponse.id

        then:
        folderResponse
        folderResponse.label == 'Test child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Test child folder'
    }

    void 'change child folder label'() {
        when:
        Folder folderResponse = (Folder) PUT("/folders/$folderId/folders/$childFolderId", [label: 'Updated child folder'], Folder)

        then:
        folderResponse
        folderResponse.label == 'Updated child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Updated child folder'
    }

    void 'change parent folder label'() {
        when:
        Folder folderResponse = (Folder) PUT("/folders/$folderId", [label: 'Updated folder'], Folder)

        then:
        folderResponse
        folderResponse.id == folderId
        folderResponse.label == 'Updated folder'
        folderResponse.path.toString() == 'fo:Updated folder'
    }

    void 'list folders'() {
        when:
        ListResponse<Folder> folderListResponse = (ListResponse<Folder>) GET('/folders', ListResponse<Folder>)


        then:
        folderListResponse
        folderListResponse.count == 2
        folderListResponse.items.path.sort().collect { it.toString()} == ['fo:Updated folder', 'fo:Updated folder|fo:Updated child folder']
    }
}
