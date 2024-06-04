package uk.ac.ox.softeng.mauro.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest

@SecuredContainerizedTest
class ReaderAccessIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID readersGroupId

    void 'reader can read but not delete or edit a folder'() {
        given:
        loginAdmin()
        Folder folder = (Folder) POST('/folders', [label: 'Admin folder'], Folder)
        folderId = folder.id

        UserGroup editorsGroup = (UserGroup) POST('/userGroups', [name: 'Readers Group'], UserGroup)
        readersGroupId = editorsGroup.id

        CatalogueUser catalougeUserResponse = PUT("/catalogueUsers/$user.id", [groups: [readersGroupId]], CatalogueUser)

        SecurableResourceGroupRole securableResourceGroupRole = (SecurableResourceGroupRole) POST("/folder/$folderId/roles/Reader/userGroups/$readersGroupId", null, SecurableResourceGroupRole)

        loginUser()

        when:
        folder = (Folder) GET("/folders/$folderId", Folder)

        then:
        folder
        folder.label == 'Admin folder'

        when:
        folder = (Folder) PUT("/folders/$folderId", [description: 'Updated'], Folder)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DELETE("/folders/$folderId")

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader role is inherited on datamodel from folder'() {
        given:
        loginAdmin()
        DataModel dataModel = (DataModel) POST("/folders/$folderId/dataModels", [label: 'Admin data model'], DataModel)
        dataModelId = dataModel.id

        loginUser()

        when:
        dataModel = (DataModel) GET("/dataModels/$dataModelId", DataModel)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = (DataModel) PUT("/dataModels/$dataModelId", [description: 'Updated'], DataModel)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DELETE("/dataModels/$dataModelId")

        then: 'deleting models requires container administrator role'
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader actions are forbidden when securable resource group role is deleted'() {
        given:
        loginAdmin()
        DataModel dataModel = (DataModel) POST("/folders/$folderId/dataModels", [label: 'Admin data model'], DataModel)
        dataModelId = dataModel.id
        DELETE("/folder/$folderId/roles/Reader/userGroups/$readersGroupId", HttpStatus)

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
        DELETE("/folders/$folderId")

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

        when:
        DELETE("/dataModels/$dataModelId")

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader role can be assigned directly at datamodel level'() {
        given:
        loginAdmin()
        POST("/dataModel/$dataModelId/roles/Reader/userGroups/$readersGroupId", null, SecurableResourceGroupRole)

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
        DELETE("/folders/$folderId")

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DataModel dataModel = (DataModel) GET("/dataModels/$dataModelId", DataModel)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = (DataModel) PUT("/dataModels/$dataModelId", [description: 'Updated again'], DataModel)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DELETE("/dataModels/$dataModelId")

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}
