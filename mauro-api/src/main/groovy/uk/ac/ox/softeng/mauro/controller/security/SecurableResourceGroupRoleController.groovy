package uk.ac.ox.softeng.mauro.controller.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
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
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class SecurableResourceGroupRoleController extends ItemController<SecurableResourceGroupRole> {

    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    @Inject
    RepositoryService repositoryService

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    SecurableResourceGroupRoleController(ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository) {
        super(securableResourceGroupRoleRepository)
        this.securableResourceGroupRoleRepository = securableResourceGroupRoleRepository
    }

    @Post('/{securableResourceDomainType}/{securableResourceId}/roles/{role}/userGroups/{userGroupId}')
    SecurableResourceGroupRole create(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId) {
        AdministeredItem securableResource = readAdministeredItem(securableResourceDomainType, securableResourceId)

        if (securableResource instanceof Folder) {
            accessControlService.checkRole(Role.CONTAINER_ADMIN, securableResource)
        } else {
            accessControlService.checkRole(Role.EDITOR, securableResource)
        }

        UserGroup userGroup = userGroupRepository.readById(userGroupId)
        if (!userGroup) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'User Group not found by ID')
        }

        SecurableResourceGroupRole securableResourceGroupRole = new SecurableResourceGroupRole(
                securableResourceDomainType: securableResourceDomainType,
                securableResourceId: securableResourceId,
                role: role,
                userGroup: userGroup
        )

        securableResourceGroupRoleRepository.save(securableResourceGroupRole)
    }

    protected AdministeredItem readAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem administeredItem = administeredItemRepository.readById(domainId)
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }

    protected AdministeredItemCacheableRepository getAdministeredItemRepository(String domainType) {
        AdministeredItemCacheableRepository administeredItemRepository = repositoryService.getAdministeredItemRepository(domainType)
        if (!administeredItemRepository) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        administeredItemRepository
    }
}
