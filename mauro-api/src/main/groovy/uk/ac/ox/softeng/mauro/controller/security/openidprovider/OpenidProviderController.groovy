package uk.ac.ox.softeng.mauro.controller.security.openidprovider

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidConnectProvider
import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidProviderApi
import uk.ac.ox.softeng.mauro.audit.Audit

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
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class OpenidProviderController implements OpenidProviderApi {

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

    @Audit
    @Get(Paths.OPENID_PROVIDER_LIST)
    List<OpenidConnectProvider> list() {
        OpenidConnectProvider openidConnectProvider = new OpenidConnectProvider(openidProviderId, label, standardProvider, authorizationEndpoint,
                imageUrl)
        [openidConnectProvider]
    }

}
