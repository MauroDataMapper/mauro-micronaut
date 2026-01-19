package org.maurodata.controller.security

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import org.maurodata.api.Paths
import org.maurodata.api.security.SecurableResourceGroupRoleApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.security.SecurableResourceGroupRoleRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SecurableResourceGroupRoleController extends ItemController<SecurableResourceGroupRole> implements SecurableResourceGroupRoleApi {

    @Inject
    SecurableResourceGroupRoleRepository securableResourceGroupRoleRepositoryUncached

    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    SecurableResourceGroupRoleController(ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository) {
        super(securableResourceGroupRoleRepository)
        this.securableResourceGroupRoleRepository = securableResourceGroupRoleRepository
    }

    @Get(Paths.SECURABLE_RESOURCE_GROUP_ROLES)
    ListResponse<SecurableResourceGroupRole> listSecurableResourceGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @Nullable PaginationParams params = new PaginationParams()) {
        AdministeredItem securableResource = readAdministeredItem(securableResourceDomainType, securableResourceId)
        accessControlService.checkRole(Role.READER, securableResource)
        ListResponse.from(securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(securableResource.domainType, securableResource.id), params)
    }


    @Get(Paths.GROUP_ROLES)
    ListResponse<Role.RoleDTO> listGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @Nullable PaginationParams params = new PaginationParams()) {
        ListResponse.from(Role.values().collect { role ->
            new Role.RoleDTO(role)
        }, params)
    }



    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.SECURABLE_ROLE_GROUP_ID)
    SecurableResourceGroupRole create(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId) {
        AdministeredItem securableResource = readAdministeredItem(securableResourceDomainType, securableResourceId)

        checkCanEditRoleOnItem(role, securableResource)

        UserGroup userGroup = userGroupRepository.readById(userGroupId)
        if (!userGroup) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'User Group not found by ID')
        }

        // Check we don't already have a role assigned for this group
        List<SecurableResourceGroupRole> existing = securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(securableResource.domainType, securableResource.id)
        if(existing.find {it.userGroup.id == userGroupId}) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'A role for this group already exists')
        }

        SecurableResourceGroupRole securableResourceGroupRole = new SecurableResourceGroupRole(
                securableResourceDomainType: securableResource.domainType,
                securableResourceId: securableResource.id,
                role: role,
                userGroup: userGroup
        )

        SecurableResourceGroupRole securableResourceGroupRole2 = securableResourceGroupRoleRepository.save(securableResourceGroupRole)

        return securableResourceGroupRole2
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.SECURABLE_ROLE_GROUP_ID)
    HttpResponse delete(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId) {
        AdministeredItem securableResource = readAdministeredItem(securableResourceDomainType, securableResourceId)

        checkCanEditRoleOnItem(role, securableResource)

        Long deleted = securableResourceGroupRoleRepository.deleteBySecurableResourceDomainTypeAndSecurableResourceIdAndRoleAndUserGroupId(securableResource.domainType, securableResource.id, role, userGroupId)

        if (deleted) {
            HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    protected checkCanEditRoleOnItem(Role role, AdministeredItem securableResource) {
        if (securableResource instanceof Folder) {
            accessControlService.checkRole(Role.CONTAINER_ADMIN, securableResource)
        } else {
            accessControlService.checkRole(Role.EDITOR, securableResource)
            if (role >= Role.CONTAINER_ADMIN) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Role CONTAINER_ADMIN is only applicable to Containers')
            }
        }
    }
}
