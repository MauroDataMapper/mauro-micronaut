package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.api.security.LoginApi

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.security.authentication.UsernamePasswordCredentials
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.security.ApplicationRole
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.security.utils.SecureRandomStringGenerator
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

abstract class SecuredIntegrationSpec extends CommonDataSpec {

    @Inject LoginApi loginApi

    @Inject
    @Shared
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository

    @Inject
    @Shared
    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    @Shared
    CatalogueUser adminUser

    @Shared
    CatalogueUser user

    void setupSpec() {
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
            salt: SecureRandomStringGenerator.generateSalt()
        )

        this.adminUser = catalogueUserRepository.save(adminUser)

        CatalogueUser user = new CatalogueUser(
            emailAddress: 'user@example.com',
            firstName: 'Test',
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

        this.user = catalogueUserRepository.save(user)

        UserGroup administrators = new UserGroup(
            name: 'Administrators',
            undeletable: true,
            applicationRole: ApplicationRole.ADMIN
        )

        administrators = userGroupRepository.save(administrators)

        userGroupRepository.addCatalogueUser(administrators.id, adminUser.id)
    }

    HttpResponse loginUsernamePassword(String username, String password) {
        HttpResponse response = client.toBlocking().exchange(HttpRequest.POST(
            '/authentication/login',
            [
                username: username,
                password: password
            ]
        ), Map<String, Object>)
        sessionCookie = response.getCookie('SESSION').get()
        response
    }


    CatalogueUser loginAdmin() {
        loginApi.login(new UsernamePasswordCredentials('admin@example.com', 'password'))
    }

    CatalogueUser loginUser() {
        loginApi.login(new UsernamePasswordCredentials('user@example.com', 'password'))
    }

    void logout() {
        loginApi.logout()
 /*
        try {
            GET('/authentication/logout')
        } catch (HttpClientResponseException exception) {
            if (exception.status != HttpStatus.OK) {
                throw exception
            }
        }
        // TODO : Do we need to clear the session cookie now?
        sessionCookie = null

  */
    }
}
