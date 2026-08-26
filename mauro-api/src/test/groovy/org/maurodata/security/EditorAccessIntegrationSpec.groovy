package org.maurodata.security

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.security.Role

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.SecuredContainerizedTest

@SecuredContainerizedTest
@Singleton
class EditorAccessIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID editorsGroupId

    void 'editor can read and edit but not delete a folder'() {
        given:
        loginAdmin()
        Folder folder = folderApi.create(new Folder(label: 'Admin folder'))
        folderId = folder.id

        UserGroup editorsGroup = userGroupApi.create(new UserGroup(name: 'Editors Group'))
        editorsGroupId = editorsGroup.id

        CatalogueUser catalogueUserResponse = catalogueUserApi.update(user.id, new CatalogueUser(groups: [editorsGroupId]))

        SecurableResourceGroupRole securableResourceGroupRole = securableResourceGroupRoleApi.create("folder", folderId, Role.EDITOR, editorsGroupId)

        loginUser()

        when:
        folder = folderApi.show(folderId)

        then:
        folder
        folder.label == 'Admin folder'

        when:
        folder = folderApi.update(folderId, new Folder(description: 'Updated'))

        then:
        folder
        folder.description == 'Updated'

        when:
        folderApi.delete(folderId, new Folder(),true)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'editor role is inherited on datamodel from folder'() {
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
        dataModel
        dataModel.description == 'Updated'

        when:
        dataModelApi.delete(dataModelId, new DataModel(),true)

        then: 'deleting models requires container administrator role'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'editor actions are forbidden when securable resource group role is deleted'() {
        given:
        loginAdmin()
        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Admin data model'))
        dataModelId = dataModel.id
        securableResourceGroupRoleApi.delete("folder", folderId, Role.EDITOR, editorsGroupId)

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

    void 'editor role can be assigned directly at datamodel level'() {
        given:
        loginAdmin()
        securableResourceGroupRoleApi.create("dataModel", dataModelId,  Role.EDITOR, editorsGroupId)

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
        dataModel
        dataModel.description == 'Updated again'

        when:
        dataModelApi.delete(dataModelId, new DataModel(),true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'editors and administrators cannot edit a finalised model or model component'() {
        given:
        loginAdmin()
        Folder folder = folderApi.create(new Folder(label: 'Admin folder'))
        folderId = folder.id

        UserGroup editorsGroup = userGroupApi.create(new UserGroup(name: 'Editors Group 2'))
        editorsGroupId = editorsGroup.id

        CatalogueUser catalogueUserResponse = catalogueUserApi.update(user.id, new CatalogueUser(groups: [editorsGroupId]))

        SecurableResourceGroupRole securableResourceGroupRole = securableResourceGroupRoleApi.create("folder", folderId, Role.EDITOR, editorsGroupId)

        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'New Data Model'))
        DataClass dataClass = dataClassApi.create(dataModel.id, new DataClass(label: 'New Data Class'))

        dataModelApi.finalise(dataModel.id, new FinaliseData(version: ModelVersion.from( '1.0.0')))

        when:
        dataModelApi.update(dataModel.id, new DataModel(label: 'Changed Data Model'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:

        dataClassApi.update(dataModel.id, dataClass.id, new DataClass(label: 'Changed Data Class'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        loginUser()

        dataModelApi.update(dataModel.id, new DataModel(label: 'Changed Data Model'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:

        dataClassApi.update(dataModel.id, dataClass.id, new DataClass(label: 'Changed Data Class'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

    }

}
