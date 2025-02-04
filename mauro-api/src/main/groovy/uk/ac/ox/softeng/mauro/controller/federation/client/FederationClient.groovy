package uk.ac.ox.softeng.mauro.controller.federation.client

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueAuthenticationType

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.exceptions.HttpException
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Slf4j
@CompileStatic
@Singleton
class FederationClient {

    @Client(id = FederationClientConfiguration.PREFIX, configuration = FederationClientConfiguration.class)
    HttpClient httpClient

    private final FederationClientConfiguration federationClientConfiguration

    private String contextPath
    private Map headersMap = [:]

    @Inject
    FederationClient(FederationClientConfiguration federationClientConfiguration) {
        this.federationClientConfiguration = federationClientConfiguration
    }

    void clientSetup(SubscribedCatalogue subscribedCatalogue) {
        this.contextPath = ClientContext.resolveContextPath(subscribedCatalogue)
        this.httpClient = getFederatedClient(subscribedCatalogue)
        log.debug("FederationClient: no previous state: setting context path $contextPath")

        switch (subscribedCatalogue.subscribedCatalogueAuthenticationType) {
            case SubscribedCatalogueAuthenticationType.API_KEY:
                headersMap.put(FederationClientConfiguration.API_KEY_HEADER, subscribedCatalogue.apiKey)
                break
            default:
                break
        }
    }

    Map<String, Object> fetchFederatedClientDataAsMap(SubscribedCatalogue subscribedCatalogue, String requestPath) {
        clientSetup(subscribedCatalogue)
        String contextAndPath = resolvePath(requestPath)
        try {
            httpClient.toBlocking().retrieve((HttpRequest.GET(contextAndPath?.toURI())
                .headers(headersMap)), Map<String, Object>)
        }
        catch (HttpException ex) {
            log.error(ex.message)
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, ex.message)
        }
    }

    byte[] retrieveBytesFromClient(SubscribedCatalogue subscribedCatalogue, String url) {
        clientSetup(subscribedCatalogue)
        try {
            httpClient.toBlocking().retrieve(HttpRequest.GET(url.toURI()).headers(headersMap), Argument.of(byte[])) as byte[]
        }
        catch (HttpException ex) {
            log.error(ex.message)
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, ex.message)
        }
    }

    private String resolvePath(String requestPath) {
        return contextPath + requestPath
    }


    private HttpClient getFederatedClient(SubscribedCatalogue subscribedCatalogue) {
        return new DefaultHttpClient(subscribedCatalogue.url.toURI(), federationClientConfiguration)
    }
}