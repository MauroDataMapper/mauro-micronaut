package uk.ac.ox.softeng.mauro.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.util.Toggleable
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.*
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository

@CompileStatic
@Singleton
@Slf4j
class AccessControlService implements Toggleable {

    @Inject
    @Nullable
    SecurityService securityService

    @Inject
    PathRepository pathRepository

    @Inject
    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    @Inject
    CatalogueUserCacheableRepository catalogueUserRepository

    @Property(name = 'micronaut.security.enabled', defaultValue = 'true')
    private boolean enabled

    /**
     * Check that a user is logged in.
     * @return if logged in, throw AuthorizationException otherwise
     */
    void checkAuthenticated() {
        if (!enabled) return

        if (!userAuthenticated) throw new AuthorizationException(null)
    }

    /**
     * Check that a user is logged in and is an Administrator.
     * @return if an admin is logged in, throw AuthorizationException otherwise
     */
    void checkAdministrator() {
        checkAuthenticated()

        if (!administrator) {
            throw new AuthorizationException(null)
        }
    }

    /**
     * @return true if user is an admin, false otherwise
     */
    boolean isAdministrator() {
        if (!isUserAuthenticated()) return false

        List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(userId)

        if (userGroups.any {UserGroup userGroup -> userGroup.applicationRole == ApplicationRole.ADMIN}) {
            return true
        }

        false
    }

    /**
     * For a given Role and an AdministeredItem, check if the current authenticated user can do the role on the item,
     * by checking permissions on the owner or inherited from any of its parents.
     * @return if authorised, throw AuthorizationException otherwise
     */
    void checkRole(@NonNull Role role, @NonNull AdministeredItem item) {
        if (!enabled) return

        pathRepository.readParentItems(item)
        Model owner = item.owner

        if (!canDoRole(role, owner)) throw new AuthorizationException(userAuthentication)
    }

    /**
     * For a given Role and a Model, check if the current authenticated user can do the role on the model, by checking
     * permissions on the model or inherited from any of its parents.
     * @return true if authorised, false otherwise
     */
    boolean canDoRole(@NonNull Role role, @NonNull Model model) {
        if (!enabled) return true
        if (userAuthenticated && model.catalogueUser.id == getUserId()) return true

        if (role <= Role.READER) {
            if (model.readableByEveryone) return true
            if (model.readableByAuthenticatedUsers && userAuthenticated) return true
        }

        List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(userId)
        List<Model> parentModels = pathRepository.readParentItems(model) as List<Model>

        parentModels.any {canDoRoleWithGroups(role, userGroups, it)}
    }

    /**
     * For a given Role, list of UserGroups, and a Model, check if a user who has membership in userGroups can do the
     * role on the model, checking the permissions on the specific model only.
     * @return true if authorised, false otherwise
     */
    private boolean canDoRoleWithGroups(Role role, List<UserGroup> userGroups, Model model) {
        List<SecurableResourceGroupRole> securableResourceGroupRoles = securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(model.domainType, model.id)
        boolean canDoRole = securableResourceGroupRoles.find {
            SecurableResourceGroupRole securableResourceGroupRole -> role <= securableResourceGroupRole.role && securableResourceGroupRole.userGroup.id in userGroups.id
        }
        canDoRole
    }

    boolean isUserAuthenticated() {
        securityService.authenticated && userAuthentication.attributes.id instanceof UUID
    }

    Authentication getUserAuthentication() {
        if (!securityService.authenticated) {
            throw new AuthenticationException('User is not authenticated')
        }
        securityService.authentication.get()
    }

    UUID getUserId() {
        (UUID) userAuthentication.attributes.id
    }

    CatalogueUser getUser() {
        catalogueUserRepository.findById(userId)
    }

    boolean isEnabled() {
        enabled
    }
}
