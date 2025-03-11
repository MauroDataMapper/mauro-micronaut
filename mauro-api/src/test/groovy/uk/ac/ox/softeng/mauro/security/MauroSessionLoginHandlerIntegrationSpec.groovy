package uk.ac.ox.softeng.mauro.security

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpMethod
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.cookie.Cookies
import io.micronaut.http.simple.SimpleHttpRequest
import io.micronaut.http.uri.UriBuilder
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.ServerAuthentication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.authentication.MauroSessionLoginHandler

@SecuredContainerizedTest
class MauroSessionLoginHandlerIntegrationSpec extends SecuredIntegrationSpec {

    static String KEYCLOAK_LOGIN_PATH = '/oauth/login/keycloak'

    @Inject
    @Shared
    MauroSessionLoginHandler mauroSessionLoginHandler

    @Shared
    SimpleHttpRequestMock httpRequestMock


    void setupSpec() {
        httpRequestMock = new SimpleHttpRequestMock(HttpMethod.GET, KEYCLOAK_LOGIN_PATH, null)

    }

    void 'keycloak login as valid admin user - should login success'() {
        given:
        Authentication authentication = new ServerAuthentication(UUID.randomUUID().toString(), [], [id: adminUser.id])

        when:
        MutableHttpResponse<?> mutableHttpResponse = mauroSessionLoginHandler.loginSuccess(authentication, httpRequestMock)

        then:
        mutableHttpResponse

    }


    @Replaces(SimpleHttpRequest)
    class SimpleHttpRequestMock extends SimpleHttpRequest {

        SimpleHttpRequestMock(HttpMethod method, String url, Object body) {
            super(method, url, body)
        }

        @Override
        HttpMethod getMethod() {
            return HttpMethod.GET
        }

        @Override
        Cookies getCookies() {
            return cookies
        }

        @Override
        URI getUri() {
            return UriBuilder.of(KEYCLOAK_LOGIN_PATH).build()
        }
    }

}
