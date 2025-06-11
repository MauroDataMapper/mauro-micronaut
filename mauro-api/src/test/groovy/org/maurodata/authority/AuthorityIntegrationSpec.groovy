package org.maurodata.authority

import org.maurodata.domain.authority.Authority
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-authority.sql"], phase = Sql.Phase.AFTER_ALL)
class AuthorityIntegrationSpec extends SecuredIntegrationSpec {


    void 'user not signed in - cannot access authority endpoint'() {
        when:
        authorityApi.list()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'any user can read authority item'() {
        loginUser()

        when:
        ListResponse<Authority> authorityListResponse = authorityApi.list()
        then:
        authorityListResponse
        authorityListResponse.items.size() == 1
        Authority defaultAuthority = authorityListResponse.items.first()

        when:
        Authority retrievedById = authorityApi.show(defaultAuthority.id)
        then:
        retrievedById

        logout()
        loginAdmin()
        when:
        retrievedById = authorityApi.show(defaultAuthority.id)

        then:
        retrievedById

        logout()
    }


    void ' only Admin User can create authority '() {
        given:
        loginAdmin()

        when:
        Authority created = authorityApi.create(authorityPayload())

        then:
        created
        created.defaultAuthority == false

        logout()
        loginUser()

        when:
        authorityApi.create(authorityPayload())
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

    }

    void ' only Admin user can update authority'() {
        given:
        loginAdmin()

        Authority created = authorityApi.create(authorityPayload())

        logout()

        loginUser()

        when:
        authorityApi.update(created.id, new Authority(label: 'updated authority payload'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()
        when:
        Authority updated = authorityApi.update(created.id, new Authority(label: 'updated authority payload'))

        then:
        updated
        updated.label == 'updated authority payload'
        updated.defaultAuthority == created.defaultAuthority
        updated.defaultAuthority == false
        updated.url == created.url

        logout()
    }

    void ' only Admin user can delete authority'() {
        given:
        loginAdmin()
        Authority created = authorityApi.create(authorityPayload())

        logout()
        loginUser()

        when:
        authorityApi.delete(created.id, new Authority())

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()

        when:
        HttpResponse httpResponse = authorityApi.delete(created.id, new Authority())

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        logout()
    }

}
