package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.domain.security.Role

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest

@SecuredContainerizedTest
@Singleton
class ReaderAccessIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID readersGroupId

    void 'reader can read but not delete or edit a folder'() {
        given:
        loginAdmin()
        Folder folder = folderApi.create(new Folder(label: 'Admin folder'))
        folderId = folder.id

        UserGroup editorsGroup = userGroupApi.create(new UserGroup(name: 'Readers Group'))
        readersGroupId = editorsGroup.id

        CatalogueUser catalogueUserResponse = catalogueUserApi.update(user.id, new CatalogueUser(groups: [readersGroupId]))

        SecurableResourceGroupRole securableResourceGroupRole = securableResourceGroupRoleApi.create("folder", folderId, Role.READER, readersGroupId)

        loginUser()

        when:
        folder = folderApi.show(folderId)

        then:
        folder
        folder.label == 'Admin folder'

        when:
        folder = folderApi.update(folderId, new Folder(description: 'Updated'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        folderApi.delete(folderId, new Folder(),true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader role is inherited on datamodel from folder'() {
        given:
        loginAdmin()
        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Admin data model'))
        dataModelId = dataModel.id

        loginUser()

        when:
        dataModel = dataModelApi.show(dataModelId)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = dataModelApi.update(dataModelId, new DataModel(description: 'Updated'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        dataModelApi.delete(dataModelId, new DataModel(),true)

        then: 'deleting models requires container administrator role'
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader actions are forbidden when securable resource group role is deleted'() {
        given:
        loginAdmin()
        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Admin data model'))
        dataModelId = dataModel.id
        securableResourceGroupRoleApi.delete("folder", folderId, Role.READER, readersGroupId)

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
        folderApi.delete(folderId, new Folder(),true)

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

        when:
        dataModelApi.delete(dataModelId, new DataModel(),true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'reader role can be assigned directly at datamodel level'() {
        given:
        loginAdmin()
        securableResourceGroupRoleApi.create("dataModel", dataModelId, Role.READER, readersGroupId)

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
        folderApi.delete(folderId, new Folder(),true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DataModel dataModel = dataModelApi.show(dataModelId)

        then:
        dataModel
        dataModel.label == 'Admin data model'

        when:
        dataModel = dataModelApi.update(dataModelId, new DataModel(description: 'Updated again'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        dataModelApi.delete(dataModelId, new DataModel(),true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}
