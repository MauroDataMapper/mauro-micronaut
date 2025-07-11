package org.maurodata.security

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
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.*
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import org.maurodata.persistence.model.PathRepository

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
    boolean enabled

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
        if (!enabled) return

        checkAuthenticated()

        if (!administrator) {
            throw new AuthorizationException(userAuthentication)
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

        // if item is null, allow access to continue, e.g. to return a not found message
        if (!item) return

        if (!canDoRole(role, item)) throw new AuthorizationException(userAuthentication)
    }


    /**
     * For a given Role and an AdministeredItem, return if the current authenticated user can do the role on the item,
     * by checking permissions on the item or inherited from any of its parents.
     * @return true if authorised, false otherwise
     */
    boolean canDoRole(@NonNull Role role, @NonNull AdministeredItem item) {
        if (!enabled) return true
        if (isAdministrator()) return true // always allow Administrator full access
        pathRepository.readParentItems(item)
        Model owner = item.owner
        if (userAuthenticated && owner.catalogueUser && owner.catalogueUser.id == getUserId()) return true // always allow owner full access

        List<Model> parentModels = pathRepository.readParentItems(owner) as List<Model>

        // allow Reader access if owning model or parents are publicly readable
        if (role <= Role.READER &&
            parentModels.any {Model model ->
                model.readableByEveryone || (model.readableByAuthenticatedUsers && userAuthenticated)
            }) {
            return true
        }

        if (!userAuthenticated) return false

        // allow role access according to owning model or parents
        List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(userId)
        parentModels.any {canDoRoleWithGroups(role, userGroups, it)}
    }

    /**
     * For all roles and an AdministeredItem, list the available roles (in permission order)
     * the current authenticated user is authorised to apply to the item
     * @return the list of Role
     */
    List<Role> listCanDoRoles(@NonNull AdministeredItem item)
    {
        final List<Role> allRoles=Arrays.asList(Role.values())

        // All roles
        if (!enabled) return allRoles
        if (isAdministrator()) return allRoles
        pathRepository.readParentItems(item)
        final Model owner = item.owner
        if (userAuthenticated && owner.catalogueUser && owner.catalogueUser.id == getUserId()) return allRoles

        // Permitted roles
        final List<Model> parentModels = pathRepository.readParentItems(owner) as List<Model>
        List<UserGroup> userGroups = []
        if(userAuthenticated) {
            userGroups = userGroupRepository.readAllByCatalogueUserId(userId)
        }

        final List<Role> canDo=[];

        for(Role role : allRoles)
        {
            if (role <= Role.READER &&
                    parentModels.any {Model model ->
                        model.readableByEveryone || (model.readableByAuthenticatedUsers && userAuthenticated)
                    }){
                canDo.add(role)
            }

            if (!userAuthenticated) break;

            if( parentModels.any {canDoRoleWithGroups(role, userGroups, it)} )
            {
                canDo.add(role)
            }
        }

        return canDo
    }

    boolean isAuthenticatedAdministrator() {
        userAuthenticated && isAdministrator()
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
        if (!enabled) return
        if (!securityService.authenticated) {
            throw new AuthenticationException('User is not authenticated')
        }
        catalogueUserRepository.findById(userId)
    }

    boolean isEnabled() {
        enabled
    }

    /**
     * For a given Role and an AdministeredItem, check if the current authenticated user can do the role on the item,
     * by checking permissions on the owner or inherited from any of its parents.
     * @return if authorised, throw AuthorizationException otherwise
     */
    void checkAdminOrUser(UUID catalogueUserId = null) {
        if (!enabled) return

        if (!securityService.authenticated) {
            throw new AuthenticationException('User is not authenticated')
        }

        if(administrator)
            return

        if(catalogueUserId && user.id == catalogueUserId)
            return


        throw new AuthorizationException(userAuthentication)
    }
}
