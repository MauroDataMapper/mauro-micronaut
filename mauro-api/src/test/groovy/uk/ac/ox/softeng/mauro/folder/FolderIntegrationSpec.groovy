package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.domain.authority.Authority

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
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
        folderResponse.authority
    }

    void 'create child folder'() {
        when:
        Folder folderResponse = (Folder) POST("/folders/$folderId/folders", [label: 'Test child folder'], Folder)
        childFolderId = folderResponse.id

        then:
        folderResponse
        folderResponse.label == 'Test child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Test child folder'
        folderResponse.authority
    }

    void 'change child folder label'() {
        when:
        Folder folderResponse = (Folder) PUT("/folders/$folderId/folders/$childFolderId", [label: 'Updated child folder'], Folder)

        then:
        folderResponse
        folderResponse.label == 'Updated child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Updated child folder'
        folderResponse.authority
    }

    void 'change parent folder label'() {
        when:
        Folder folderResponse = (Folder) PUT("/folders/$folderId", [label: 'Updated folder'], Folder)

        then:
        folderResponse
        folderResponse.id == folderId
        folderResponse.label == 'Updated folder'
        folderResponse.path.toString() == 'fo:Updated folder'
        folderResponse.authority
    }

    void 'list folders'() {
        when:
        ListResponse<Folder> folderListResponse = (ListResponse<Folder>) GET('/folders', ListResponse, Folder)

        then:
        folderListResponse
        folderListResponse.count == 2
        folderListResponse.items.path.collect { it.toString()}.sort() == ['fo:Updated folder', 'fo:Updated folder|fo:Updated child folder']
    }
}
