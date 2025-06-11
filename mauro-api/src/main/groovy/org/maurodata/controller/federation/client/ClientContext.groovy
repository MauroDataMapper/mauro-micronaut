package org.maurodata.controller.federation.client

import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedCatalogueType

import groovy.transform.CompileStatic

@Singleton
@CompileStatic
class ClientContext {

    static String resolveContextPath(SubscribedCatalogue subscribedCatalogue) {
        URI hostUri = subscribedCatalogue.url.toURI()
        if (subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MAURO_JSON) {
            String hostUriPath = formattedPath(hostUri.path)

            if (!hostUriPath.endsWith(FederationClientConfiguration.API_PATH)) hostUriPath = hostUriPath + FederationClientConfiguration.API_PATH
            hostUriPath
        } else {
            hostUri.path
        }
    }

    static String formattedPath(String source) {
        return source.endsWith('/') ? source : "${source}/"
    }
}
