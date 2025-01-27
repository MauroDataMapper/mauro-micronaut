package uk.ac.ox.softeng.mauro.config

import uk.ac.ox.softeng.mauro.api.config.ApiPropertyApi
import uk.ac.ox.softeng.mauro.api.security.LoginApi
import uk.ac.ox.softeng.mauro.domain.config.ApiProperty

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import io.micronaut.security.authentication.UsernamePasswordCredentials
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
class ApiPropertyIntegrationSpec extends SecuredIntegrationSpec {

    @Inject ApiPropertyApi apiPropertyApi

    @Shared
    UUID apiPropertyId

    void 'only admin can create an api property'() {
        given:

        loginAdmin()

        when:
        ApiProperty apiProperty = apiPropertyApi.create(new ApiProperty(
            key: 'org.example.key',
            value: 'test value',
            publiclyVisible: true,
            category: 'test category'))

        apiPropertyId = apiProperty.id
        then:
        apiProperty
        apiProperty.key == 'org.example.key'
        apiProperty.value == 'test value'
        apiProperty.publiclyVisible == true
        apiProperty.category == 'test category'

        when:
        loginUser()
        apiPropertyApi.create(new ApiProperty(
            key: 'org.example.key2',
            value: 'test value',
            publiclyVisible: true,
            category: 'test category'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'anonymous user can read all public api properties'() {
        when:
        ListResponse response = apiPropertyApi.listPubliclyVisible()

        then:
        response
        response.count == 1

        when:
        ApiProperty apiProperty = response.items.first()

        then:
        apiProperty.key == 'org.example.key'
        apiProperty.value == 'test value'
        apiProperty.publiclyVisible == true
        apiProperty.category == 'test category'
    }

    void 'updating api property updates list'() {
        given:
        loginAdmin()

        when:
        ApiProperty response =
            apiPropertyApi.update(apiPropertyId, new ApiProperty(publiclyVisible: false, value: 'updated'))

        then:
        response
        response.key == 'org.example.key'
        response.value == 'updated'
        response.publiclyVisible == false
        response.category == 'test category'

        when:
        ListResponse allResponse = apiPropertyApi.listPubliclyVisible()

        then:
        allResponse.count == 0
        allResponse.items.isEmpty()

        when:
        allResponse = apiPropertyApi.listAll()

        then:
        allResponse
        allResponse.count == 1

        when:
        ApiProperty apiProperty = allResponse.items.first()

        then:
        apiProperty.key == 'org.example.key'
        apiProperty.value == 'updated'
        apiProperty.publiclyVisible == false
        apiProperty.category == 'test category'
    }
}
