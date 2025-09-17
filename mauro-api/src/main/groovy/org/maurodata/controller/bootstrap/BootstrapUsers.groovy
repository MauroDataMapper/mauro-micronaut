package org.maurodata.controller.bootstrap

import org.maurodata.domain.authority.Authority
import org.maurodata.domain.config.ApiProperty
import org.maurodata.domain.security.ApiKey
import org.maurodata.domain.security.ApplicationRole
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.ApiPropertyCacheableRepository
import org.maurodata.persistence.config.ApiPropertyRepository
import org.maurodata.security.utils.SecureRandomStringGenerator
import org.maurodata.security.utils.SecurityUtils
import org.maurodata.service.core.AuthorityService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.core.annotation.Order
import io.micronaut.core.order.Ordered
import io.micronaut.discovery.event.ServiceReadyEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.text.StringEscapeUtils

@CompileStatic
@Slf4j
@Singleton
class BootstrapUsers implements ApplicationEventListener<ServiceReadyEvent> {

    @Inject
    MauroConfiguration mauroConfiguration

    @Inject
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository

    @Inject
    ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository

    @Inject

    ApiPropertyCacheableRepository apiPropertyCacheableRepository

    @Override
    void onApplicationEvent(final ServiceReadyEvent event) {

        // Create groups
        if (mauroConfiguration.groups != null) {

            mauroConfiguration.groups.forEach {

                List<UserGroup> existingUserGroups = userGroupRepository.readAllByName(it.name)

                if (existingUserGroups.isEmpty()) {
                    UserGroup userGroup = new UserGroup(
                        name: it.name,
                        description: it.description ?: '',
                        applicationRole: it.isAdmin ? ApplicationRole.ADMIN : null,
                        undeletable: false
                    )

                    userGroupRepository.save(userGroup)

                    log.info("Created user group ${it.name}")
                } else {
                    log.info("User group ${it.name} exists")
                }
            }
        }

        // Create users
        if (mauroConfiguration.users != null) {
            mauroConfiguration.users.forEach {

                CatalogueUser catalogueUser = catalogueUserRepository.readByEmailAddress(it.email)

                if (catalogueUser == null) {
                    CatalogueUser newUser = new CatalogueUser(
                        emailAddress: it.email,
                        firstName: it.firstName,
                        lastName: it.lastName,
                        tempPassword: it.tempPassword,
                        pending: false,
                        disabled: false,
                        creationMethod: "BOOTSTRAP",
                        catalogueUser: null,
                        salt: SecureRandomStringGenerator.generateSalt(),
                        password: null
                    )
                    catalogueUserRepository.save(newUser)
                    log.info("Created user ${it.email}")
                } else {
                    log.info("User ${it.email} exists")
                }
            }
        }

        // Members of groups
        if (mauroConfiguration.groups != null) {

            mauroConfiguration.groups.forEach {MauroConfiguration.UserGroupConfig userGroupConfig ->

                if (userGroupConfig.members != null) {

                    List<UserGroup> existingUserGroups = userGroupRepository.readAllByName(userGroupConfig.name)

                    if (!existingUserGroups.isEmpty()) {

                        Iterator<String> configMembers = userGroupConfig.members.iterator()
                        while (configMembers.hasNext()) {
                            String configMember = configMembers.next()

                            CatalogueUser configCatalogueUser = catalogueUserRepository.readByEmailAddress(configMember)
                            if (configCatalogueUser == null) {
                                log.warn("The User ${configMember} does not exist. Cannot be added to user group ${userGroupConfig.name}")
                                continue
                            }

                            List<UUID> existingUserGroupIds = userGroupRepository.readAllByCatalogueUserId(configCatalogueUser.id).id

                            // Establish whether they are a member of a group with this name or not
                            boolean isAMember = false

                            Iterator<UserGroup> userGroups = existingUserGroups.iterator()

                            while (userGroups.hasNext()) {
                                UserGroup userGroup = userGroups.next()

                                if (existingUserGroupIds.contains(userGroup.id)) {
                                    isAMember = true
                                    break
                                }
                            }

                            if (!isAMember) {
                                // Add them to the first group with this name
                                UserGroup userGroupToAddTo = existingUserGroups.get(0)
                                userGroupRepository.addCatalogueUser(userGroupToAddTo.id, configCatalogueUser.id)
                                log.info("Added the User ${configMember} to the user group ${userGroupConfig.name}")
                            } else {
                                log.info("The User ${configMember} is a member of the user group ${userGroupConfig.name}")
                            }
                        }
                    }
                }
            }
        }

        if (mauroConfiguration.apiKeys != null) {

            Iterator<MauroConfiguration.ApiKeyConfig> apiKeyConfigs = mauroConfiguration.apiKeys.iterator()
            while (apiKeyConfigs.hasNext()) {
                MauroConfiguration.ApiKeyConfig apiKeyConfig = apiKeyConfigs.next()

                CatalogueUser configCatalogueUser = catalogueUserRepository.readByEmailAddress(apiKeyConfig.email)
                if (configCatalogueUser == null) {
                    log.warn("The User ${apiKeyConfig.email} does not exist. Cannot create an api key")
                    continue
                }

                List<ApiKey> userNamedApiKeys = apiKeyCacheableRepository.readByCatalogueUserIdAndName(configCatalogueUser.id, apiKeyConfig.name)
                ApiKey keyById
                if (apiKeyConfig.key != null) {
                    keyById = apiKeyCacheableRepository.readById(apiKeyConfig.key)
                } else {
                    keyById = null
                }

                if (userNamedApiKeys.isEmpty() && keyById == null) {

                    log.info("apiKeyConfig.key " + apiKeyConfig.key?.toString())

                    ApiKey newApiKey = new ApiKey(
                        name: apiKeyConfig.name,
                        expiryDate: apiKeyConfig.expiry,
                        refreshable: apiKeyConfig.refreshable,
                        disabled: false,
                        catalogueUserId: configCatalogueUser.id,
                        catalogueUser: configCatalogueUser,
                        id: apiKeyConfig.key != null ? apiKeyConfig.key : UUID.randomUUID()
                    )

                    log.info("newApiKey.id " + newApiKey.id?.toString())

                    apiKeyCacheableRepository.save(newApiKey)
                    log.info("Created key '${newApiKey.name}' ${newApiKey.id.toString()} for user ${apiKeyConfig.email}")
                } else {
                    if (!userNamedApiKeys.isEmpty()) {
                        log.info("The User ${apiKeyConfig.email} has a key '${apiKeyConfig.name}' ${userNamedApiKeys.get(0).id.toString()}")
                    } else {
                        log.warn(
                            "A different user than ${apiKeyConfig.email} has a key ${apiKeyConfig.key.toString()} - it is already in use. Choose a different key, or leave " +
                            "that field blank and it will be chosen for you")
                    }
                }
            }
        }

        if (mauroConfiguration.apiProperties != null) {

            Iterator<MauroConfiguration.ApiPropertyConfig> apiPropertyConfigs = mauroConfiguration.apiProperties.iterator()
            while (apiPropertyConfigs.hasNext()) {
                MauroConfiguration.ApiPropertyConfig apiPropertyConfig = apiPropertyConfigs.next()

                ApiProperty apiProperty = apiPropertyCacheableRepository.findByKey(apiPropertyConfig.key)

                if (apiProperty != null) {
                    log.info("There is an api-property for '${apiProperty.key}'")
                } else {

                    apiProperty = new ApiProperty(

                        key: apiPropertyConfig.key,
                        value: StringEscapeUtils.unescapeJava(apiPropertyConfig.value),
                        publiclyVisible: apiPropertyConfig.publiclyVisible != null ? apiPropertyConfig.publiclyVisible : false,
                        category: apiPropertyConfig.category != null && !apiPropertyConfig.category.trim().isEmpty() ? apiPropertyConfig.category : "Mauro"
                    )

                    apiPropertyCacheableRepository.save(apiProperty)
                    log.info("Created api-property key '${apiProperty.key}'")
                    log.info(apiProperty.value)
                }
            }
        }
    }
}
