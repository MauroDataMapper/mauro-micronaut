package uk.ac.ox.softeng.mauro.controller.security.openidprovider

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
class OpenidProviderController {


    @Value('${micronaut.security.openid-provider.id:b2092268-3014-4c24-a9cc-0913b2c63cfb}')
    String openidProviderId

    @Value('${micronaut.security.openid-provider.label:Keycloak}')
    String label

    @Value('${micronaut.security.openid-provider.standardProvider:true}')
    boolean standardProvider

    @Value('${micronaut.security.openid-provider.authorizationEndpoint:http://tvssnsdedm001.oxnet.nhs.uk:8095/realms/test/protocol/openid-connect/auth?scope=openid+email+profile&response_type=code&client_id=mdm&state=3b2e1535-5cae-49f7-83c3-0032aa7539c9&nonce=6ea24466-f8f4-3089-8881-e6b76eb70d5a}')
    String authorizationEndpoint
    @Value('${micronaut.security.openid-provider.imageUrl:https://upload.wikimedia.org/wikipedia/commons/2/29/Keycloak_Logo.png?20200311211229}')
    String imageUrl

    @Get()
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
            this.openidProviderId = UUID.fromString(openidProviderId)
            this.label = label
            this.standardProvider = standardProvider
            this.authorizationEndpoint = authorizationEndpoint
            this.imageUrl = imageUrl
        }
    }
}
