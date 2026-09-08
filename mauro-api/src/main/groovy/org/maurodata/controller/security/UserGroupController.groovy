package org.maurodata.controller.security

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Delete
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.api.Paths
import org.maurodata.api.security.UserGroupApi
import org.maurodata.audit.Audit
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.cache.ItemCacheableRepository

import jakarta.inject.Inject

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class UserGroupController extends ItemController<UserGroup> implements UserGroupApi {

    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository
    @Inject
    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    UserGroupController(ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository) {
        super(userGroupRepository)
        this.userGroupRepository = userGroupRepository
    }

    @Transactional
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(operationId = 'createUserGroup', summary = "Create a user group", description = "Creates a user group. It is only available to administrator users.")
    @Post(Paths.USER_GROUP_LIST)
    UserGroup create(@Body @NonNull UserGroup userGroup) {
        accessControlService.checkAdministrator()
        userGroupRepository.save(userGroup)
    }

    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(operationId = 'deleteUserGroup', summary = "Delete a user group", description = "Deletes a user group. It is only available to administrator users.")
    @Delete(Paths.USER_GROUP_ID)
    HttpResponse delete(UUID id, @Body @Nullable UserGroup userGroup) {
        accessControlService.checkAdministrator()
        UserGroup userGroupToDelete = userGroupRepository.findById(id)
        List<SecurableResourceGroupRole> securableResourceGroupRoleList =
            securableResourceGroupRoleRepository.readAllByUserGroupIdIn([id])
        if(securableResourceGroupRoleList.size() > 0) {
            securableResourceGroupRoleRepository.deleteAll (securableResourceGroupRoleList)
        }

        if (userGroupToDelete?.version) {
            userGroupToDelete.version = userGroup.version
        }
        Long deleted = userGroupRepository.delete(userGroup)
        if (deleted) {
            return HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    @Audit
    @Operation(operationId = 'indexUserGroup', summary = "List the user groups", description = "Returns the user groups. Its availability is governed by access control checks on the requested resource.")
    @Get(Paths.USER_GROUP_LIST)
    ListResponse<UserGroup> index(@Nullable PaginationParams params = new PaginationParams()) {
        if (!accessControlService.administrator) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }

        return ListResponse.from(userGroupRepository.readAll(),params)
    }

    @Operation(operationId = 'showUserGroup', summary = "Get a user group", description = "Returns a user group. It is available to authenticated users.")
    @Get(Paths.USER_GROUP_ID)
    UserGroup show(UUID id) {
        accessControlService.checkAuthenticated()

        if (!accessControlService.administrator && accessControlService.userId != id) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }

        UserGroup userGroup = userGroupRepository.findById(id)

        userGroup.availableActions=["delete","show","update"]

        userGroup
    }

    @Audit
    @Operation(summary = "List the user groups", description = "Returns the user groups. It is available to authenticated users.")
    @Get(Paths.USER_GROUP_CATALOGUE_USERS_PAGED)
    ListResponse<CatalogueUser> users(UUID id, @Nullable PaginationParams params){

        accessControlService.checkAuthenticated()

        if (!accessControlService.administrator) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }
        List<CatalogueUser> usersInGroup = catalogueUserRepository.readAllByUserGroupId(id)
        return ListResponse.from(usersInGroup, params)
    }

    @Audit
    @Operation(operationId = 'updateUserGroup', summary = "Update a user group", description = "Updates a user group. Its availability is governed by access control checks on the requested resource.")
    @Put(Paths.USER_GROUP_ID)
    UserGroup update(@NonNull UUID id, @Body @NonNull UserGroup userGroup){
        log.info 'Request to update UserGroup by ID'

        if (!accessControlService.administrator) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }

        userGroup.availableActions = null
        userGroup.groupMembers = null

        cleanBody(userGroup, false)

        UserGroup existing = userGroupRepository.readById(id)

        boolean hasChanged = updateProperties(existing, userGroup)

        if (hasChanged) {
            userGroupRepository.update(existing)
        }

        userGroup
    }

    /*
    HERE: USER_GROUP_ID_CATALOGUE_USERS_ID PUT DELETE returns UserGroup with availableActions
    
     */
}
