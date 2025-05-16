package uk.ac.ox.softeng.mauro.controller.security

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.security.SecurableResourceGroupRoleApi
import uk.ac.ox.softeng.mauro.audit.Audit

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
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SecurableResourceGroupRoleController extends ItemController<SecurableResourceGroupRole> implements SecurableResourceGroupRoleApi {

    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    SecurableResourceGroupRoleController(ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository) {
        super(securableResourceGroupRoleRepository)
        this.securableResourceGroupRoleRepository = securableResourceGroupRoleRepository
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

        SecurableResourceGroupRole securableResourceGroupRole = new SecurableResourceGroupRole(
                securableResourceDomainType: securableResource.domainType,
                securableResourceId: securableResource.id,
                role: role,
                userGroup: userGroup
        )

        securableResourceGroupRoleRepository.save(securableResourceGroupRole)
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
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Role CONTAINER_ADMIN is only applicable to Containers')
            }
        }
    }
}
