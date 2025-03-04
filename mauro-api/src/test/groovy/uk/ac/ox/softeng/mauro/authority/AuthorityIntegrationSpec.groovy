package uk.ac.ox.softeng.mauro.authority

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-authority.sql"], phase = Sql.Phase.AFTER_ALL)
class AuthorityIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application


    void 'user not signed in - cannot access authority endpoint'() {
        when:
        GET(AUTHORITIES_PATH, ListResponse<Authority>)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'any user can read authority item'() {
        loginUser()

        when:
        ListResponse<Authority> authorityListResponse = (ListResponse<Authority>) GET(AUTHORITIES_PATH, ListResponse, Authority)
        then:
        authorityListResponse
        authorityListResponse.items.size() == 1
        Authority defaultAuthority = authorityListResponse.items.first()

        when:
        Authority retrievedById = (Authority) GET("$AUTHORITIES_PATH/$defaultAuthority.id", Authority)
        then:
        retrievedById

        logout()
        loginAdmin()
        when:
        retrievedById = (Authority) GET("$AUTHORITIES_PATH/$defaultAuthority.id", Authority)

        then:
        retrievedById

        logout()
    }


    void ' only Admin User can create authority '() {
        given:
        loginAdmin()

        when:
        Authority created = (Authority) POST(AUTHORITIES_PATH, authorityPayload(), Authority)

        then:
        created
        created.defaultAuthority == false

        logout()
        loginUser()

        when:
        POST(AUTHORITIES_PATH, authorityPayload())
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

    }

    void ' only Admin user can update authority'() {
        given:
        loginAdmin()

        Authority created = (Authority) POST(AUTHORITIES_PATH, authorityPayload(), Authority)

        logout()

        loginUser()

        when:
        PUT("$AUTHORITIES_PATH/$created.id", [label: 'updated authority payload'], Authority)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()
        when:
        Authority updated = (Authority) PUT("$AUTHORITIES_PATH/$created.id", [label: 'updated authority payload'], Authority)

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
        Authority created = (Authority) POST(AUTHORITIES_PATH, authorityPayload(), Authority)

        logout()
        loginUser()

        when:
        DELETE("$AUTHORITIES_PATH/$created.id", HttpStatus)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()

        when:
        HttpStatus httpStatus = DELETE("$AUTHORITIES_PATH/$created.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        logout()
    }

}
