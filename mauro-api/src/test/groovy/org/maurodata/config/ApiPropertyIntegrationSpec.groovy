package org.maurodata.config

import org.maurodata.api.config.ApiPropertyApi
import org.maurodata.api.security.LoginApi
import org.maurodata.domain.config.ApiProperty

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import io.micronaut.security.authentication.UsernamePasswordCredentials
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse

@SecuredContainerizedTest
@Singleton
class ApiPropertyIntegrationSpec extends SecuredIntegrationSpec {

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
