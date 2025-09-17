package org.maurodata.controller.security

import org.maurodata.api.Paths
import org.maurodata.api.security.UserGroupApi
import org.maurodata.audit.Audit
import org.maurodata.domain.security.CatalogueUser
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

    UserGroupController(ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository) {
        super(userGroupRepository)
        this.userGroupRepository = userGroupRepository
    }

    @Transactional
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.USER_GROUP_LIST)
    UserGroup create(@Body @NonNull UserGroup userGroup) {
        accessControlService.checkAdministrator()
        userGroupRepository.save(userGroup)
    }

    @Audit
    @Get(Paths.USER_GROUP_LIST)
    ListResponse<UserGroup> index(@Nullable PaginationParams params = new PaginationParams()) {
        if (!accessControlService.administrator) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }

        return ListResponse.from(userGroupRepository.readAll(),params)
    }

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
