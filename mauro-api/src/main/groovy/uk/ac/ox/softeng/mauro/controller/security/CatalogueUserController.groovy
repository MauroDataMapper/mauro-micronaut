package uk.ac.ox.softeng.mauro.controller.security

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.security.CatalogueUserApi

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.security.utils.SecureRandomStringGenerator
import uk.ac.ox.softeng.mauro.security.utils.SecurityUtils
import uk.ac.ox.softeng.mauro.web.ChangePassword

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class CatalogueUserController extends ItemController<CatalogueUser> implements CatalogueUserApi {

    CatalogueUserCacheableRepository catalogueUserRepository

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    CatalogueUserController(CatalogueUserCacheableRepository catalogueUserRepository) {
        super(catalogueUserRepository)
        this.catalogueUserRepository = catalogueUserRepository
    }

    @Override
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['emailAddress', 'pending', 'disabled', 'resetToken', 'creationMethod', 'lastLogin', 'salt', 'password', 'tempPassword']
    }

    @Post(Paths.USER_ADMIN_REGISTER)
    CatalogueUser adminRegister(@Body @NonNull CatalogueUser newUser) {
        log.info 'Request to register a new user by admin'
        cleanBody(newUser)

        accessControlService.checkAdministrator()

        newUser.pending = false
        newUser.disabled = false
        newUser.creationMethod = 'ADMIN_REGISTER'
        newUser.catalogueUser = accessControlService.user
        newUser.tempPassword = SecurityUtils.generateRandomPassword()
        newUser.salt = SecureRandomStringGenerator.generateSalt()
        newUser.password = null

        catalogueUserRepository.save(newUser)
    }

    @Get(Paths.USER_CURRENT_USER)
    CatalogueUser currentUser() {
        log.info 'Request to get current logged in user'

        accessControlService.user
    }

    @Put(Paths.USER_CHANGE_PASSWORD)
    CatalogueUser changePassword(@Body @NonNull ChangePassword changePasswordRequest) {
        log.info 'Request by user to change own password'

        accessControlService.checkAuthenticated()

        CatalogueUser currentUser = accessControlService.user

        currentUser.password = SecurityUtils.getHash(changePasswordRequest.newPassword, currentUser.salt)
        currentUser.tempPassword = null

        catalogueUserRepository.update(currentUser)
    }

    @Put(Paths.USER_ID)
    CatalogueUser update(@NonNull UUID id, @Body @NonNull CatalogueUser catalogueUser) {
        log.info 'Request to update CatalogueUser by ID'

        accessControlService.checkAuthenticated()

        Set<UserGroup> newGroups = catalogueUser.groups
        catalogueUser.groups = null
        cleanBody(catalogueUser)
        CatalogueUser existing = catalogueUserRepository.readById(id)

        if (!accessControlService.administrator && accessControlService.userId != existing.id) {
            throw new AuthorizationException(accessControlService.userAuthentication)
        }

        // Only an Administrator can add a user to Groups
        if (newGroups) {
            accessControlService.checkAdministrator()
        }

        boolean hasChanged = updateProperties(existing, catalogueUser)

        if (hasChanged) {
            catalogueUserRepository.update(existing)
        }

        if (newGroups) {
            List<UUID> existingUserGroupIds = userGroupRepository.readAllByCatalogueUserId(existing.id).id

            List<UUID> newUserGroupIds = newGroups.collect {it.id}.findAll {it !in existingUserGroupIds}

            newUserGroupIds.each {UUID userGroupId ->
                userGroupRepository.addCatalogueUser(userGroupId, existing.id)
            }
        }

        existing
    }

    // todo Stub method to enable login with UI
    @Get(Paths.USER_PREFERENCES)
    String showUserPreferences(UUID id) {
        ''
    }
}
