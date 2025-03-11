package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

import uk.ac.ox.softeng.mauro.domain.authority.Authority

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Singleton
class FolderIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID childFolderId


    void 'create folder'() {
        when:
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        folderId = folderResponse.id

        then:
        folderResponse
        folderResponse.label == 'Test folder'
        folderResponse.path.toString() == 'fo:Test folder'
        folderResponse.authority
    }

    void 'create child folder'() {
        when:
        Folder folderResponse = folderApi.create(folderId, new Folder(label: 'Test child folder'))
        childFolderId = folderResponse.id

        then:
        folderResponse
        folderResponse.label == 'Test child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Test child folder'
        folderResponse.authority
    }

    void 'change child folder label'() {
        when:
        Folder folderResponse = folderApi.update(folderId, childFolderId, new Folder(label: 'Updated child folder'))

        then:
        folderResponse
        folderResponse.label == 'Updated child folder'
        folderResponse.path.toString() == 'fo:Test folder|fo:Updated child folder'
        folderResponse.authority
    }

    void 'change parent folder label'() {
        when:
        Folder folderResponse = folderApi.update(folderId, new Folder(label: 'Updated folder'))

        then:
        folderResponse
        folderResponse.id == folderId
        folderResponse.label == 'Updated folder'
        folderResponse.path.toString() == 'fo:Updated folder'
        folderResponse.authority
    }

    void 'list folders'() {
        when:
        ListResponse<Folder> folderListResponse = folderApi.listAll()

        then:
        folderListResponse
        folderListResponse.count == 2
        folderListResponse.items.path.collect { it.toString()}.sort() == ['fo:Updated folder', 'fo:Updated folder|fo:Updated child folder']
    }
}
