package uk.ac.ox.softeng.mauro.controller.security.openidprovider

import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidConnectProvider
import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidProviderApi

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Slf4j
@Controller('/openidConnectProviders')
@Secured(SecurityRule.IS_ANONYMOUS)
class OpenidProviderController implements OpenidProviderApi {

    @Value('${mauro.oauth.id}')
    String openidProviderId

    @Value('${mauro.oauth.label}')
    String label

    @Value('${mauro.oauth.standard-provider}')
    boolean standardProvider

    @Value('${mauro.oauth.authorization-endpoint}')
    String authorizationEndpoint

    @Value('${mauro.oauth.image-url}')
    String imageUrl

    @Get
    List<OpenidConnectProvider> list() {
        OpenidConnectProvider openidConnectProvider = new OpenidConnectProvider(openidProviderId, label, standardProvider, authorizationEndpoint,
                imageUrl)
        [openidConnectProvider]
    }


}
