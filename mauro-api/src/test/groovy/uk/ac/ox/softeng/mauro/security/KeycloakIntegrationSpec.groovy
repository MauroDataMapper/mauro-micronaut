package uk.ac.ox.softeng.mauro.security

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpStatus
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Form
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.htmlunit.FailingHttpStatusCodeException
import org.htmlunit.SilentCssErrorHandler
import org.htmlunit.WebClient
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import spock.lang.Ignore
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import java.nio.charset.Charset
@Ignore
@SecuredContainerizedTest
class KeycloakIntegrationSpec extends BaseIntegrationSpec {

    static String PASSWORD_GRANT_TYPE = 'password'
    static String OPEN_ID = 'openid'
    @Shared
    String tokenEndpoint = 'http://localhost:9009/realms/mauro/protocol/openid-connect/token'

    @Shared
    @Value('${micronaut.security.oauth2.clients.keycloak.client-id}')
    String clientId

    @Shared
    @Value('${micronaut.security.oauth2.clients.keycloak.client-secret}')
    String clientSecret

    @Shared
    String mauroRealmUser = 'researcher.one@sdetest.com'

    @Shared
    String mauroRealmPassword = 'password'

    @Shared
    EmbeddedServer app = ApplicationContext.run(EmbeddedServer)


    void 'login'() {
        given:
        client.getProperties()

        WebClient webClient = createWebClient()
        String loginUrl = "http://localhost:${app.port}/oauth/login/keycloak"

        when:
        webClient.getPage(loginUrl)

        then:
        FailingHttpStatusCodeException exception = thrown()
        exception.statusCode == HttpStatus.BAD_REQUEST.code
        exception.getLocalizedMessage().contains("Bad Request for http://localhost:9009/realms/mauro/protocol/openid-connect/auth")
        exception.getResponse().getContentAsString(Charset.defaultCharset()).contains("http://localhost:8088")
    }

    void 'can access token endpoint from keycloak instance - should return tokens'() {
        //using Resteasy for URLENCODED form support
        Entity entity = Entity.form(createForm())
        ResteasyClient resteasyClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).build()
        ResteasyWebTarget target = resteasyClient.target(tokenEndpoint)

        when:
        Response resp = target.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .post(entity)

        then:
        resp
        Map<String, String> responseEntity = resp.readEntity(Map.class)
        resp.status == HttpStatus.OK.code

        def currentAccessToken = responseEntity.get('access_token')
        currentAccessToken
    }

    static WebClient createWebClient() {
        WebClient webClient = new WebClient()
        webClient.setCssErrorHandler(new SilentCssErrorHandler())
        webClient
    }

    protected Form createForm() {
        Form form = new Form()
        form.param('grant_type', PASSWORD_GRANT_TYPE)
        form.param('client_id', clientId)
        form.param('client_secret', clientSecret)
        form.param('username', mauroRealmUser)
        form.param('password', mauroRealmPassword)
        form.param('scope', OPEN_ID)
        form
    }

}
