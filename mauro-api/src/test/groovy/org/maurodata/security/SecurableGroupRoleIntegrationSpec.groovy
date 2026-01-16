package org.maurodata.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.web.ListResponse
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
class SecurableGroupRoleIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    void setup() {
        loginAdmin()
        Folder folder = folderApi.create(new Folder(label: 'Folder with Metadata'))
        folderId = folder.id
        logout()
    }

    void 'list initial roles state'() {
        when:
            loginAdmin()
            ListResponse<SecurableResourceGroupRole> securableResourceGroupRoleList = securableResourceGroupRoleApi.listSecurableResourceGroupRoles("folder", folderId)
            ListResponse<UserGroup> userGroupList = userGroupApi.index(null)
            ListResponse<Role.RoleDTO> roleList = securableResourceGroupRoleApi.listGroupRoles("folder", folderId)

        then:
            securableResourceGroupRoleList.count == 0
        and:
            userGroupList.count == 1
            userGroupList.items.find {
                it.name == 'Administrators'
            }
        and:
        roleList.count == 5
        Role.values().each {role ->
            roleList.find {it.name == role.name()}
        }

    }

    void 'add group roles and remove them'() {
        when:
        loginAdmin()
        ListResponse<SecurableResourceGroupRole> securableResourceGroupRoleList = securableResourceGroupRoleApi.listSecurableResourceGroupRoles("folder", folderId)
        ListResponse<UserGroup> userGroupList = userGroupApi.index(null)

        then:
        securableResourceGroupRoleList.count == 0
        userGroupList.count == 1

        when:
        UserGroup administratorsUserGroup = userGroupList.items.first()

        securableResourceGroupRoleApi.create("folder", folderId, Role.EDITOR, administratorsUserGroup.id)
        securableResourceGroupRoleList = securableResourceGroupRoleApi.listSecurableResourceGroupRoles("folder", folderId)

        then:
        securableResourceGroupRoleList.count == 1
        securableResourceGroupRoleList.items.first().userGroup.id == administratorsUserGroup.id
        securableResourceGroupRoleList.items.first().role == Role.EDITOR

        // Now try and add a different role for the same user group - should fail
        when:
        securableResourceGroupRoleApi.create("folder", folderId, Role.CONTAINER_ADMIN, administratorsUserGroup.id)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST


        when:
        securableResourceGroupRoleApi.delete("folder", folderId, Role.EDITOR, administratorsUserGroup.id)
        securableResourceGroupRoleList = securableResourceGroupRoleApi.listSecurableResourceGroupRoles("folder", folderId)

        then:
        securableResourceGroupRoleList.count == 0

    }

}
