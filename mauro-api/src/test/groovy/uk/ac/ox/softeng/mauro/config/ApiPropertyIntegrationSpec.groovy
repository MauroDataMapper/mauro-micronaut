package uk.ac.ox.softeng.mauro.config

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
class ApiPropertyIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID apiPropertyId

    void 'only admin can create an api property'() {
        given:
        loginAdmin()

        when:
        Map<String, Object> response = POST('/admin/properties', [
            key: 'org.example.key',
            value: 'test value',
            publiclyVisible: true,
            category: 'test category'
        ])
        apiPropertyId = UUID.fromString(response.id)

        then:
        response
        response.key == 'org.example.key'
        response.value == 'test value'
        response.publiclyVisible == true
        response.category == 'test category'

        when:
        loginUser()
        POST('/admin/properties', [
            key: 'org.example.key2',
            value: 'test value',
            publiclyVisible: true,
            category: 'test category'
        ])

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'anonymous user can read all public api properties'() {
        when:
        ListResponse response = GET('/properties')

        then:
        response
        response.count == 1

        when:
        Map apiProperty = response.items.first()

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
        Map<String, Object> response = PUT("/admin/properties/$apiPropertyId", [publiclyVisible: false, value: 'updated'])

        then:
        response
        response.key == 'org.example.key'
        response.value == 'updated'
        response.publiclyVisible == false
        response.category == 'test category'

        when:
        ListResponse allResponse = GET('/properties')

        then:
        allResponse.count == 0
        allResponse.items.isEmpty()

        when:
        allResponse = GET('/admin/properties')

        then:
        allResponse
        allResponse.count == 1

        when:
        Map apiProperty = allResponse.items.first()

        then:
        apiProperty.key == 'org.example.key'
        apiProperty.value == 'updated'
        apiProperty.publiclyVisible == false
        apiProperty.category == 'test category'
    }
}
