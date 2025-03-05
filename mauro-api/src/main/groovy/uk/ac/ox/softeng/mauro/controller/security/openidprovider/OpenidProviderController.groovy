package uk.ac.ox.softeng.mauro.controller.security.openidprovider

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Slf4j
@Controller('/openidConnectProviders')
@Secured(SecurityRule.IS_ANONYMOUS)
class OpenidProviderController {
    @Nullable
    @Value('${mauro.oauth.id}')
    String openidProviderId

    @Nullable
    @Value('${mauro.oauth.label}')
    String label

    @Nullable
    @Value('${mauro.oauth.standard-provider}')
    Boolean standardProvider

    @Nullable
    @Value('${mauro.oauth.authorization-endpoint}')
    String authorizationEndpoint

    @Nullable
    @Value('${mauro.oauth.image-url}')
    String imageUrl

    @Get
    List<OpenidConnectProvider> list() {
        OpenidConnectProvider openidConnectProvider = new OpenidConnectProvider(openidProviderId, label, standardProvider, authorizationEndpoint,
                imageUrl)
        [openidConnectProvider]
    }


    class OpenidConnectProvider {
        UUID openidProviderId
        String label
        boolean standardProvider
        String authorizationEndpoint
        String imageUrl

        OpenidConnectProvider(String openidProviderId, String label, boolean standardProvider,
                              String authorizationEndpoint, String imageUrl) {
            if (openidProviderId) this.openidProviderId = UUID.fromString(openidProviderId)
            if ( label) this.label = label
            this.standardProvider = standardProvider
            if ( authorizationEndpoint) this.authorizationEndpoint = authorizationEndpoint
            if (imageUrl) this.imageUrl = imageUrl
        }
    }
}
