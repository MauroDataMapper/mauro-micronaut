package uk.ac.ox.softeng.mauro.controller.federation.client

import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton

import java.time.Duration

@Slf4j
@CompileStatic
@Singleton
class FederationClient {

    @Client(id = FederationClientConfiguration.PREFIX, configuration = FederationClientConfiguration.class)
    HttpClient httpClient

    protected String contextPath
    protected Map headersMap = [:]

    void initHttpClient(SubscribedCatalogue subscribedCatalogue, FederationClientConfiguration federationClientConfiguration) {
        if (subscribedCatalogue.connectTimeout) federationClientConfiguration.connectTimeout = Duration.ofMinutes(subscribedCatalogue.connectTimeout)

        this.contextPath = resolveContextPath(subscribedCatalogue)
        this.httpClient = new DefaultHttpClient(subscribedCatalogue.url.toURI(), federationClientConfiguration)

        switch (subscribedCatalogue.subscribedCatalogueAuthenticationType) {
            case SubscribedCatalogueAuthenticationType.API_KEY:
                headersMap.put(FederationClientConfiguration.API_KEY_HEADER, subscribedCatalogue.apiKey)
                break
            default:
                break
        }
    }

    Map<String, Object> fetchFederatedClientDataAsMap(String requestPath) {
        String contextAndPath = resolvePath(requestPath)
        try {
            httpClient.toBlocking().retrieve((HttpRequest.GET(contextAndPath.toURI())
                .headers(headersMap)), Map<String, Object>)
        }
        catch (HttpException ex) {
            log.error(ex.message)
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }
    }

    protected String resolvePath(String requestPath) {
        return contextPath + requestPath
    }

    protected static String resolveContextPath(SubscribedCatalogue subscribedCatalogue) {
        URI hostUri = subscribedCatalogue.url.toURI()
        if (subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MAURO_JSON) {
            String hostUriPath = formattedPath(hostUri.path)

            if (!hostUriPath.endsWith(FederationClientConfiguration.API_PATH)) hostUriPath = hostUriPath + FederationClientConfiguration.API_PATH
            hostUriPath
        } else {
            hostUri.path
        }
    }

    protected static String formattedPath(String source) {
        return source.endsWith('/') ? source : "${source}/"
    }
}
