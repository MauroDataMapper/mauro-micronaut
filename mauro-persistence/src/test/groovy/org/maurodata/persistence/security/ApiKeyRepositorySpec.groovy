package org.maurodata.persistence.security

import org.maurodata.domain.security.ApiKey
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.ItemCacheableRepository.ApiKeyCacheableRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

@ContainerizedTest
class ApiKeyRepositorySpec extends Specification {

    @Inject
    CatalogueUserRepository catalogueUserRepository

    @Inject
    ApiKeyRepository apiKeyRepository

    @Inject
    ApiKeyCacheableRepository apiKeyCacheableRepository

    @Shared
    CatalogueUser savedUser

    def setup() {
        CatalogueUser adminUser = new CatalogueUser(
            emailAddress: 'admin@example.com',
            firstName: 'Admin',
            lastName: 'User',
            jobTitle: 'Integration Spec',
            organisation: 'Mauro',
            pending: false,
            disabled: false,
            profilePicture: null,
            userPreferences: null,
            resetToken: null,
            creationMethod: 'INTEGRATION_SPEC',
            tempPassword: 'password',
            salt: 'new salt'.bytes
        )
        savedUser = catalogueUserRepository.save(adminUser)

    }

    def "Test store and retrieve api key (cacheable)"() {

        when:
        ApiKey apiKey = ApiKey.build {
            name "My first Api Key"
            disabled false
            expiryDate "2025-01-01T00:00:00.00Z"
            catalogueUserId savedUser.id
        }
        ApiKey savedApiKey = apiKeyRepository.save(apiKey)

        then:
        savedApiKey.id

        when:
        ApiKey retrievedApiKey = apiKeyRepository.findById(savedApiKey.id)

        then:
        retrievedApiKey.name == 'My first Api Key'

        when:
        List<ApiKey> allApiKeysForUser = apiKeyRepository.readByCatalogueUserId(savedUser.id)

        then:
        allApiKeysForUser.size() == 1
        allApiKeysForUser.find {it.name == 'My first Api Key'}

        when:
        apiKeyRepository.delete(savedApiKey)

        then:
        !apiKeyRepository.findById(savedApiKey.id)
    }

    def "Test store and retrieve api key"() {

        when:
        ApiKey apiKey = ApiKey.build {
            name "My first Api Key"
            disabled false
            expiryDate "2025-01-01T00:00:00.00Z"
            catalogueUserId savedUser.id
        }
        ApiKey savedApiKey = apiKeyCacheableRepository.save(apiKey)

        then:
        savedApiKey.id

        when:
        ApiKey retrievedApiKey = apiKeyCacheableRepository.findById(savedApiKey.id)

        then:
        retrievedApiKey.name == 'My first Api Key'

        when:
        List<ApiKey> allApiKeysForUser = apiKeyCacheableRepository.readByCatalogueUserId(savedUser.id)

        then:
        allApiKeysForUser.size() == 1
        allApiKeysForUser.find {it.name == 'My first Api Key'}

        when:
        apiKeyCacheableRepository.delete(savedApiKey)

        then:
        !apiKeyCacheableRepository.findById(savedApiKey.id)

    }

}
