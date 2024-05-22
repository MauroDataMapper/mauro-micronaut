package uk.ac.ox.softeng.mauro.security

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.security.ApplicationRole
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.UserGroupCacheableRepository
import uk.ac.ox.softeng.mauro.security.utils.SecureRandomStringGenerator
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

@MicronautTest(environments = 'secured')
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
@Slf4j
class LoginLogoutIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Inject
    @Shared
    CatalogueUserCacheableRepository catalogueUserRepository

    @Inject
    @Shared
    UserGroupCacheableRepository userGroupRepository

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

    void 'not logged in user is unauthorized'() {
        when:
        GET('/folders')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

        when:
        GET('/admin/modules')

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'login as admin'() {
        when:
        HttpResponse response = client.toBlocking().exchange(HttpRequest.POST(
            '/authentication/login',
            [
                username: 'admin@example.com',
                password: 'password'
            ]
        ), Map<String, Object>)
        sessionCookie = response.getCookie('SESSION').get()

        then:
        response
        response.body().emailAddress == 'admin@example.com'
    }

    void 'logged in administrator is authorized'() {
        when:
        List response = GET('/folders', List)

        then:
        response

        when:
        response = GET('/admin/modules', List)

        then:
        response
        response.size() > 4
    }

    void 'logout'() {
        when:
        GET('/authentication/logout')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.OK
    }

    void 'not logged in user is unauthorized'() {
        when:
        GET('/folders')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

        when:
        GET('/admin/modules')

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'login as non-admin user'() {
        when:
        Map<String, Object> response = POST(
            '/authentication/login',
            [
                username: 'user@example.com',
                password: 'password'
            ]
        )

        then:
        response
        response.emailAddress == 'user@example.com'
    }

    void 'logged in non-admin user is authorized for non-admin endpoints'() {
        when:
        List response = GET('/folders', List)

        then:
        response

        when:
        GET('/admin/modules', List)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'logout'() {
        when:
        GET('/authentication/logout')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.OK
    }

    void 'not logged in user is unauthorized'() {
        when:
        GET('/folders')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

        when:
        GET('/admin/modules')

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }
}
