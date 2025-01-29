package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest

@SecuredContainerizedTest
class LoginLogoutIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    void 'not logged in user can only access public endpoints'() {
        when:
        Map response = GET('/folders')

        then:
        response

        when:
        GET('/admin/modules')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'login as admin'() {
        when:
        CatalogueUser user = loginAdmin()

        then:
        user
        user.emailAddress == 'admin@example.com'
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
        logout()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.OK
    }

    void 'not logged in user can only access public endpoints'() {
        when:
        Map response = GET('/folders')

        then:
        response

        when:
        GET('/admin/modules')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'login as non-admin user'() {
        when:
        CatalogueUser user = loginUser()

        then:
        user
        user.emailAddress == 'user@example.com'
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
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'logout'() {
        when:
        logout()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.OK
    }

    void 'not logged in user can only access public endpoints'() {
        when:
        Map response = GET('/folders')

        then:
        response

        when:
        GET('/admin/modules')

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }
}
