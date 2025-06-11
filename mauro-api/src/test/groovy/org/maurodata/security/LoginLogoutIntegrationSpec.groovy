package org.maurodata.security

import org.maurodata.api.SessionHandlerClientFilter
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import org.maurodata.persistence.SecuredContainerizedTest

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
