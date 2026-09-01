package org.maurodata.security

import org.maurodata.domain.folder.Folder
import org.maurodata.exception.MauroApplicationException
import org.maurodata.domain.security.ApplicationRole
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.model.AdministeredItemRepository

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
        if (!enabled) {
            return
        }

        if (!userAuthenticated) {
            throw new AuthorizationException(null)
        }
    }

    /**
     * Check that a user is logged in and is an Administrator.
     * @return if an admin is logged in, throw AuthorizationException otherwise
     */
    void checkAdministrator() {
        if (!enabled) {
            return
        }

        checkAuthenticated()

        if (!administrator) {
            throw new AuthorizationException(userAuthentication)
        }
    }

    /**
     * @return true if user is an admin, false otherwise
     */
    boolean isAdministrator() {
        if (!isUserAuthenticated()) {
            return false
        }
        if (user.disabled) {
            return false
        }
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
        if (!enabled) {
            return
        }

        // if item is null, allow access to continue, e.g. to return a not found message
        if (!item) {
            return
        }

        if (!canDoRole(role, item)) {
            throw new AuthorizationException(userAuthentication)
        }
    }


    /**
     * For a given Role and an AdministeredItem, return if the current authenticated user can do the role on the item,
     * by checking permissions on the item or inherited from any of its parents.
     * @return true if authorised, false otherwise
     */
    boolean canDoRole(@NonNull Role role, @NonNull AdministeredItem item) {
        if (item == null || role == null) return false

        return (
            permissionsAllowAction(role, item)
                &&
            itemAllowsAction(role, item)
        )
    }

    boolean itemAllowsAction(@NonNull Role role, @NonNull AdministeredItem item) {

        List<AdministeredItem> parents = pathRepository.readParentItems(item)
        Model owningModel = item.owner

        switch(role) {
            case Role.READER:
                return true
            case Role.REVIEWER:
            case Role.AUTHOR:
            case Role.EDITOR:
            case Role.CONTAINER_ADMIN:
                return !owningModel.finalised
            default:
                return false
        }
    }

    boolean permissionsAllowAction(@NonNull Role role, @NonNull AdministeredItem item) {
        // if security is disabled, allow all actions
        if (!enabled) {
            return true
        }
        // if we're an administrator, then we can do anything
        if (isAdministrator()) {
            return true
        }

        List<AdministeredItem> parents = pathRepository.readParentItems(item)
        Model owningModel = item.owner
        if(!owningModel) {
            throw new MauroApplicationException("Item ${item.label} does not have an owner and should have one")
        }

        // We can also do anything if we created the model in question
        if (userId && owningModel.catalogueUser && owningModel.catalogueUser.id == userId) {
            return true
        }

        List<Folder> owningFolders = parents.findAll {it instanceof Folder} as List<Folder>
        List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(userId)

        switch (role) {
            case Role.READER:
                if (owningModel.readableByEveryone || owningFolders.find {it.readableByEveryone}) {
                    return true
                }
                if (owningModel.readableByAuthenticatedUsers || owningFolders.find {it.readableByAuthenticatedUsers}) {
                    return isUserAuthenticated()
                }
            case Role.REVIEWER:
            case Role.AUTHOR:
            case Role.EDITOR:
            case Role.CONTAINER_ADMIN:
            default:
                if(!userAuthenticated) {
                    return false
                }
                return canDoRoleWithGroups(role, userGroups, owningModel, owningFolders)
        }

    }

    /**
     * For all roles and an AdministeredItem, list the available roles (in permission order)
     * the current authenticated user is authorised to apply to the item
     * @return the list of Role
     */
    List<Role> listCanDoRoles(@NonNull AdministeredItem item) {
        final List<Role> allRoles = Arrays.asList(Role.values())

        // All roles
        if (!enabled) {
            return allRoles
        }
        if (isAdministrator()) {
            return allRoles
        }
        pathRepository.readParentItems(item)
        Model owner = item.owner
        if (owner.catalogueUser == null) {
            AdministeredItemRepository air = pathRepository.getRepository(owner)
            owner = air.readById(owner.id) as Model
        }
        if (userAuthenticated && owner.catalogueUser && owner.catalogueUser.id == getUserId()) {
            return allRoles
        }

        // Permitted roles
        final List<Model> parentModels = pathRepository.readParentItems(owner) as List<Model>
        final List<Folder> parentFolders = parentModels.findAll {it instanceof Folder} as List<Folder>
        List<UserGroup> userGroups = []
        if (userAuthenticated) {
            userGroups = userGroupRepository.readAllByCatalogueUserId(userId)
        }

        final List<Role> canDo = []

        for (Role role : allRoles) {
            if (role <= Role.READER &&
                parentModels.any {Model model ->
                    model.readableByEveryone || (model.readableByAuthenticatedUsers && userAuthenticated)
                }) {
                canDo.add(role)
            }

            if (!userAuthenticated) {
                break
            }

            if (canDoRoleWithGroups(role, userGroups, owner, parentFolders)) {
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
    private boolean canDoRoleWithGroups(Role role, List<UserGroup> userGroups, Model model, List<Folder> parentFolders) {
        List<SecurableResourceGroupRole> securableResourceGroupRoles = securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(model.domainType, model.id)


        boolean canDoRole = securableResourceGroupRoles.find { SecurableResourceGroupRole securableResourceGroupRole ->
             role <= securableResourceGroupRole.role && securableResourceGroupRole.userGroup.id in userGroups.id
        }

        canDoRole
    }

    boolean isUserAuthenticated() {
        securityService !=null && securityService.authenticated && userAuthentication.attributes.id instanceof UUID
    }

    Authentication getUserAuthentication() {
        if (securityService == null || !securityService.authenticated) {
            throw new AuthenticationException('User is not authenticated')
        }
        securityService.authentication.get()
    }

    UUID getUserId() {
        (UUID) userAuthentication.attributes.id
    }

    CatalogueUser getUser() throws AuthenticationException {
        if (!enabled) {
            return null
        }
        // if securityService is null, we assume security is turned off
        if (securityService && !securityService.authenticated) {
            log.debug("User is not authenticated, throwing AuthenticationException")
            throw new AuthenticationException('User is not authenticated')
        }

        CatalogueUser user = catalogueUserRepository.findById(userId)
        if (!user) {
            log.debug("User with id ${userId} not found, throwing AuthenticationException")
            throw new AuthenticationException('User not found')
        }

        return user
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
        if (!enabled) {
            return
        }

        if (!securityService.authenticated) {
            throw new AuthenticationException('User is not authenticated')
        }

        if(administrator) {
            return
        }

        if(catalogueUserId && user.id == catalogueUserId) {
            return
        }

        throw new AuthorizationException(userAuthentication)
    }
}
