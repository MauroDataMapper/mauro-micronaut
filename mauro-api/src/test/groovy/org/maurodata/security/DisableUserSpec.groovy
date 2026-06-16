package org.maurodata.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.UsernamePasswordCredentials
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.SessionHandlerClientFilter
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.security.utils.SecureRandomStringGenerator
import org.maurodata.web.ListResponse
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
class DisableUserSpec extends SecuredIntegrationSpec {

    @Inject
    @Shared
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository

    @Inject
    @Shared
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    @Shared
    CatalogueUser adminUser2

    @Inject
    SessionHandlerClientFilter sessionHandlerClientFilter

    void setupSpec() {
        adminUser2 = new CatalogueUser(
            emailAddress: 'admin2@example.com',
            firstName: 'Admin 2',
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
            salt: SecureRandomStringGenerator.generateSalt()
        )

        this.adminUser2 = catalogueUserRepository.save(adminUser2)
        userGroupRepository.addCatalogueUser(administrators.id, adminUser2.id)

    }


    void "Disable user and they can't log in"() {

        given:
        loginUser()
        logout()

        when:
        loginAdmin()
        catalogueUserApi.update(
            user.id, new CatalogueUser(id: user.id, disabled: true)
        )
        logout()

        loginUser()
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

        when:
        loginAdmin()
        catalogueUserApi.update(
            user.id, new CatalogueUser(id: user.id, disabled: false)
        )
        logout()

        loginUser()
        CatalogueUser currentUser = catalogueUserApi.currentUser()
        then:
        currentUser.id == user.id

    }

    void "Disable admin and their admin privileges are immediately disabled, and they can't log in again"() {

        given:
        loginAdmin2()

        when:
        ListResponse<CatalogueUser> allUsers = catalogueUserApi.index(null)

        then:
        allUsers.count > 0

        when:
        sessionHandlerClientFilter.withNewSession {
            loginAdmin()
            catalogueUserApi.update(adminUser2.id, new CatalogueUser(id: adminUser2.id, disabled: true))
            logout()
        }

        catalogueUserApi.index(null)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()

        loginAdmin2()

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

        when:
        loginAdmin()
        catalogueUserApi.update(adminUser2.id, new CatalogueUser(id: adminUser2.id, disabled: false))
        logout()

        loginAdmin2()
        CatalogueUser currentUser = catalogueUserApi.currentUser()
        then:
        currentUser.id == adminUser2.id
        currentUser.disabled == false

        when:
        allUsers = catalogueUserApi.index(null)
        then:
        allUsers.count > 0

    }

    void "Users cannot disable / enable themselves"() {
        when:
        loginAdmin()

        catalogueUserApi.update(adminUser.id, new CatalogueUser(id: adminUser.id, disabled: true))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginUser()
        catalogueUserApi.update(user.id, new CatalogueUser(id: user.id, disabled: true))
        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN



    }



    private CatalogueUser loginAdmin2() {
        loginApi.login(new UsernamePasswordCredentials('admin2@example.com', 'password'))
    }

}
