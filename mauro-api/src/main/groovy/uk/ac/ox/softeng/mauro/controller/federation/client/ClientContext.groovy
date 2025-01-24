package uk.ac.ox.softeng.mauro.controller.federation.client

import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType

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
