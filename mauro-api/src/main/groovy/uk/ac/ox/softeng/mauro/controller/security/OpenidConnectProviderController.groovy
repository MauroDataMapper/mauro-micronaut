package uk.ac.ox.softeng.mauro.controller.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.security.openidconnect.DiscoveryDocument
import uk.ac.ox.softeng.mauro.domain.security.openidconnect.OpenidConnectProvider
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/openidConnectProviders')
@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
class OpenidConnectProviderController {


    @Post()
    OpenidConnectProvider create(@Body @NonNull OpenidConnectProvider openidConnectProvider) {
        if (openidConnectProvider.discoveryDocumentUrl) {
            Map<String, Object> dataMap = loadDiscoveryDocumentMapFromUrl(openidConnectProvider.discoveryDocumentUrl)
            openidConnectProvider.discoveryDocument = createDiscoveryDocument(dataMap)
            openidConnectProvider
        }
    }

    @Get()
    ListResponse<OpenidConnectProvider> listAll() {
        ListResponse.from([
                label               : "Keycloak",
                imageUrl            : "https://upload.wikimedia.org/wikipedia/commons/2/29/Keycloak_Logo.png?20200311211229",
                clientId            : "Mauro",
                clientSecret        : "qyD1izafaEm2VO6JsCLrdUhU7LFmULnh",
                discoveryDocumentUrl: "http://localhost:9009/realms/master/.well-known/openid-configuration"
        ] as List)
    }

    DiscoveryDocument createDiscoveryDocument(Map<String, Object> data) {
        DiscoveryDocument document = new DiscoveryDocument()
        document.authorizationEndpoint = data.authorization_endpoint
        document.tokenEndpoint = data.token_endpoint
        document.userinfoEndpoint = data.userinfo_endpoint
        document.endSessionEndpoint = data.end_session_endpoint ?: data.revocation_endpoint
        document.jwksUri = data.jwks_uri
        log.info("document: {}", document.toString())
        document
    }

    Map<String, Object> loadDiscoveryDocumentMapFromUrl(String discoveryDocumentUrlString) {
        URL discoveryDocumentUrl = UriBuilder.of(discoveryDocumentUrlString).build().toURL()
        try {
            String baseUrl = "${discoveryDocumentUrl.protocol}://${discoveryDocumentUrl.host}"
            if (discoveryDocumentUrl.port != -1) baseUrl = "${baseUrl}:${discoveryDocumentUrl.port}"
            HttpClient client = HttpClient.create(baseUrl.toURL())
            log.info('base url: {}, discoveryDocumentUrl: {}', baseUrl, discoveryDocumentUrl.path)
            HttpRequest request = HttpRequest.GET(discoveryDocumentUrl.path).contentType(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE)
            client.toBlocking().exchange(request, Argument.mapOf(String, Object)).body()

        } catch (HttpClientResponseException responseException) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, responseException.getMessage())

        }
    }
}