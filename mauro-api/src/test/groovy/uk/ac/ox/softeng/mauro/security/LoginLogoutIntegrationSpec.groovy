package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.api.SessionHandlerClientFilter
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest

import jakarta.inject.Singleton

@SecuredContainerizedTest
@Singleton
class LoginLogoutIntegrationSpec extends SecuredIntegrationSpec {


    void 'not logged in user can only access public endpoints'() {
        when:
        ListResponse<Folder> response = folderApi.listAll()

        then:
        response

        when:
        adminApi.modules()

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
        ListResponse<Folder> response = folderApi.listAll()

        then:
        response

        when:
        def modulesResponse = adminApi.modules()

        then:
        modulesResponse
        modulesResponse.size() > 4
    }

    void 'logout'() {
        when:
        logout()
        then:
        sessionHandlerClientFilter.lastStatus == HttpStatus.OK
    }

    void 'not logged in user can only access public endpoints'() {
        when:
        ListResponse<Folder> response = folderApi.listAll()

        then:
        response

        when:
        adminApi.modules()

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
        ListResponse<Folder> response = folderApi.listAll()

        then:
        response

        when:
        adminApi.modules()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'logout'() {
        when:
        logout()

        then:
        sessionHandlerClientFilter.lastStatus == HttpStatus.OK
    }

    void 'not logged in user can only access public endpoints'() {
        when:
        ListResponse<Folder> response = folderApi.listAll()

        then:
        response

        when:
        adminApi.modules()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }
}
