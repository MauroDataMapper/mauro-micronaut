package uk.ac.ox.softeng.mauro.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.persistence.security.SecurableResourceGroupRoleRepository
import uk.ac.ox.softeng.mauro.persistence.security.UserGroupRepository

@CompileStatic
@Singleton
@Slf4j
class AccessControlService {

    @Inject
    SecurityService securityService

    @Inject
    PathRepository pathRepository

    @Inject
    SecurableResourceGroupRoleRepository securableResourceGroupRoleRepository

    @Inject
    UserGroupRepository userGroupRepository

    void checkRole(Role role, Model model) {
        if (!canDoRole(role, model)) throw new AuthorizationException(userAuthentication)
    }

    boolean canDoRole(@NonNull Role role, @NonNull Model model) {
        if (role <= Role.READER) {
            if (model.readableByEveryone) return true
            if (model.readableByAuthenticatedUsers && userAuthenticated) return true
        }

        List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(userId)
        List<Model> parentModels = pathRepository.readParentItems(model) as List<Model>

        parentModels.any {canDoRoleWithGroups(role, userGroups, it)}
    }

    private boolean canDoRoleWithGroups(Role role, List<UserGroup> userGroups, Model model) {
        List<SecurableResourceGroupRole> securableResourceGroupRoles = securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(model.domainType, model.id)
        boolean canDoRole = securableResourceGroupRoles.find {SecurableResourceGroupRole securableResourceGroupRole ->
            role <= securableResourceGroupRole.role &&
                    securableResourceGroupRole.userGroup.id in userGroups.id
        }
        canDoRole
    }

    boolean isUserAuthenticated() {
        securityService.authenticated && userAuthentication.attributes.id instanceof UUID
    }

    Authentication getUserAuthentication() {
        securityService.authentication.get()
    }

    UUID getUserId() {
        (UUID) userAuthentication.attributes.id
    }
}
