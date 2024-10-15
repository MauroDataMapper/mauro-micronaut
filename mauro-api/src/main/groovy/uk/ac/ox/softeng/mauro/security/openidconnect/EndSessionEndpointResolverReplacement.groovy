package uk.ac.ox.softeng.mauro.security.openidconnect

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.BeanContext
import io.micronaut.security.config.SecurityConfiguration
import io.micronaut.security.oauth2.client.OpenIdProviderMetadata
import io.micronaut.security.oauth2.configuration.OauthClientConfiguration
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpointResolver
import io.micronaut.security.oauth2.endpoint.endsession.request.OktaEndSessionEndpoint
import io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder
import io.micronaut.security.token.reader.TokenResolver
import jakarta.inject.Singleton

import java.util.function.Supplier

@Singleton
@CompileStatic
@Slf4j
class EndSessionEndpointResolverReplacement extends EndSessionEndpointResolver{
    private TokenResolver tokenResolver
    private SecurityConfiguration securityConfiguration
    /**
     * @param beanContext The bean context
     */
    EndSessionEndpointResolverReplacement(BeanContext beanContext, TokenResolver tokenResolver,
                                          SecurityConfiguration securityConfiguration) {
        super(beanContext)
        this.tokenResolver = tokenResolver
        this.securityConfiguration = securityConfiguration
    }

    @Override
    public Optional<EndSessionEndpoint> resolve(OauthClientConfiguration oauthClientConfiguration,
                                                Supplier<OpenIdProviderMetadata> openIdProviderMetadata, EndSessionCallbackUrlBuilder endSessionCallbackUrlBuilder) {
        log.debug("*********  calling resolve, oauthConfig: {}, openIdProviderMetadata: {}", oauthClientConfiguration.toString())

        OktaEndSessionEndpoint endpt = new OktaEndSessionEndpoint(endSessionCallbackUrlBuilder,
                oauthClientConfiguration,
                openIdProviderMetadata,
                securityConfiguration,
                tokenResolver)

        return Optional.ofNullable(endpt as EndSessionEndpoint)

    }
}
