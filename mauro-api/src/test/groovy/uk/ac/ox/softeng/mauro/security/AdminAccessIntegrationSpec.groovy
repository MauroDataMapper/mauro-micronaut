package uk.ac.ox.softeng.mauro.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
class AdminAccessIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    void 'admin can create a folder and datamodel'() {
        given:
        loginAdmin()

        when:
        Folder folder = (Folder) POST('/folders', [label: 'Admin folder'], Folder)
        folderId = folder.id

        then:
        folder
        folder.label == 'Admin folder'

        when:
        DataModel dataModel = (DataModel) POST("/folders/$folderId/dataModels", [label: 'Admin data model'], DataModel)
        dataModelId = dataModel.id

        then:
        dataModel
        dataModel.label == 'Admin data model'
    }

    void 'admin can read and update its own folder and datamodel'() {
        given:
        loginAdmin()

        when:
        Folder folder = (Folder) GET("/folders/$folderId", Folder)

        then:
        folder
        folder.label == 'Admin folder'

        when:
        folder = (Folder) PUT("/folders/$folderId", [description: 'Updated'], Folder)

        then:
        folder
        folder.description == 'Updated'

        when:
        DataModel dataModel = (DataModel) GET("/dataModels/$dataModelId", DataModel)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = (DataModel) PUT("/dataModels/$dataModelId", [description: 'Updated'], DataModel)

        then:
        dataModel
        dataModel.description == 'Updated'

    }

    void 'non-admin user cannot read admin\'s folder or datamodel'() {
        given:
        loginUser()

        when:
        GET("/folders/$folderId")

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        PUT("/folders/$folderId", [description: 'Updated'])

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        GET("/dataModels/$dataModelId")

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        PUT("/dataModels/$dataModelId", [description: 'Updated'])

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'admin can read non-admin user\'s folder or datamodel'() {
        given:
        loginUser()
        Folder folder = (Folder) POST('/folders', [label: 'User folder'], Folder)
        folderId = folder.id
        DataModel dataModel = (DataModel) POST("/folders/$folderId/dataModels", [label: 'User data model'], DataModel)
        dataModelId = dataModel.id

        when:
        loginAdmin()
        folder = (Folder) GET("/folders/$folderId", Folder)

        then:
        folder
        folder.label == 'User folder'

        when:
        folder = (Folder) PUT("/folders/$folderId", [description: 'Updated'], Folder)

        then:
        folder
        folder.description == 'Updated'

        when:
        dataModel = (DataModel) GET("/dataModels/$dataModelId", DataModel)

        then:
        dataModel
        dataModel.label == 'User data model'

        when:
        dataModel = (DataModel) PUT("/dataModels/$dataModelId", [description: 'Updated'], DataModel)

        then:
        dataModel
        dataModel.description == 'Updated'
    }

    void 'admin can search over all items'() {
        given:
        loginAdmin()

        when:
        ListResponse<SearchResultsDTO> searchResults = POST("/search", [searchTerm: 'Updated'], ListResponse<SearchResultsDTO>)

        then:
        searchResults.items.sort {[it.tsRank, it.label]}.label == ['Admin folder', 'User folder', 'Admin data model', 'User data model']
    }

    void 'non-admin user cannot search admin\'s items without permission'() {
        given:
        loginUser()

        when:
        ListResponse<SearchResultsDTO> searchResults = POST("/search", [searchTerm: 'Updated'], ListResponse<SearchResultsDTO>)

        then:
        searchResults.items.sort {[it.tsRank, it.label]}.label == ['User folder', 'User data model']
    }
}
