package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.domain.search.dto.SearchRequestDTO

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.domain.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Singleton
class AdminAccessIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    void 'admin can create a folder and datamodel'() {
        given:
        loginAdmin()

        when:
        Folder folder = folderApi.create(new Folder(label: 'Admin folder'))
        folderId = folder.id

        then:
        folder
        folder.label == 'Admin folder'

        when:
        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Admin data model'))
        dataModelId = dataModel.id

        then:
        dataModel
        dataModel.label == 'Admin data model'
    }

    void 'admin can read and update its own folder and datamodel'() {
        given:
        loginAdmin()

        when:
        Folder folder = folderApi.show(folderId)

        then:
        folder
        folder.label == 'Admin folder'

        when:
        folder = folderApi.update(folderId, new Folder(description: 'Updated'))

        then:
        folder
        folder.description == 'Updated'

        when:
        DataModel dataModel = dataModelApi.show(dataModelId)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = dataModelApi.update(dataModelId, new DataModel(description: 'Updated'))

        then:
        dataModel
        dataModel.description == 'Updated'

    }

    void 'non-admin user cannot read admin\'s folder or datamodel'() {
        given:
        loginUser()

        when:
        folderApi.show(folderId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        folderApi.update(folderId, new Folder(description: 'Updated'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        dataModelApi.show(dataModelId)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        dataModelApi.update(dataModelId, new DataModel(description: 'Updated'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'admin can read non-admin user\'s folder or datamodel'() {
        given:
        loginUser()
        Folder folder = folderApi.create(new Folder(label: 'User folder'))
        folderId = folder.id
        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'User data model'))
        dataModelId = dataModel.id

        when:
        loginAdmin()
        folder = folderApi.show(folderId)

        then:
        folder
        folder.label == 'User folder'

        when:
        folder = folderApi.update(folderId, new Folder(description: 'Updated'))

        then:
        folder
        folder.description == 'Updated'

        when:
        dataModel = dataModelApi.show(dataModelId)

        then:
        dataModel
        dataModel.label == 'User data model'

        when:
        dataModel = dataModelApi.update(dataModelId, new DataModel(description: 'Updated'))

        then:
        dataModel
        dataModel.description == 'Updated'
    }

    void 'admin can search over all items'() {
        given:
        loginAdmin()

        when:
        ListResponse<SearchResultsDTO> searchResults = searchApi.searchPost(new SearchRequestDTO(searchTerm: 'Updated'))

        then:
        searchResults.items.label == ['Admin folder', 'User folder', 'Admin data model', 'User data model']
    }

    void 'non-admin user cannot search admin\'s items without permission'() {
        given:
        loginUser()

        when:
        ListResponse<SearchResultsDTO> searchResults = searchApi.searchPost(new SearchRequestDTO(searchTerm: 'Updated'))

        then:
        searchResults.items.label == ['User folder', 'User data model']
    }
}
