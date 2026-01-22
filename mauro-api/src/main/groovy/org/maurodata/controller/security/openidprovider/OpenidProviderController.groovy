package org.maurodata.controller.security.openidprovider

import org.maurodata.api.Paths
import org.maurodata.api.security.openidprovider.OpenidConnectProvider
import org.maurodata.api.security.openidprovider.OpenidProviderApi
import org.maurodata.audit.Audit
import org.maurodata.controller.bootstrap.MauroConfiguration

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.context.ServerContextPathProvider
import io.micronaut.http.server.HttpServerConfiguration
import io.micronaut.http.server.util.HttpHostResolver
import io.micronaut.http.ssl.SslConfiguration
import io.micronaut.http.uri.UriBuilder
import io.micronaut.security.annotation.Secured
import io.micronaut.security.oauth2.url.OauthRouteUrlBuilder
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

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

    @Inject
    MauroConfiguration mauroConfiguration

    @Inject
    OauthRouteUrlBuilder oauthRouteUrlBuilder

    @Inject
    ServerContextPathProvider serverContextPathProvider

    @Inject
    HttpHostResolver httpHostResolver

    @Inject
    SslConfiguration sslConfig

    @Inject
    HttpServerConfiguration httpServerConfiguration

    @Audit
    @Get(Paths.OPENID_PROVIDER_LIST)
    List<OpenidConnectProvider> list(@Nullable HttpRequest<?> request = null) {

        boolean fromOauthSingular = openidProviderId && label && standardProvider && authorizationEndpoint
        boolean fromOauthsList = (mauroConfiguration.oauths != null && !mauroConfiguration.oauths.isEmpty())

        if (!fromOauthSingular && !fromOauthsList) {
            log.debug("No OpenID Connect Provider configured (mauro.oauth.{id, label, standard-provider, authorization-endpoint, image-url} is not set)")
            return []
        }

        if (!fromOauthsList && fromOauthSingular) {
            OpenidConnectProvider openidConnectProvider = new OpenidConnectProvider(openidProviderId, label, standardProvider, authorizationEndpoint,
                                                                                    imageUrl)
            return [openidConnectProvider]
        }

        if (fromOauthsList) {

            final List<OpenidConnectProvider> openidConnectProviders = []

            // Get the base scheme, host, port so that the local starting authorization endpoint
            // can be used as an external absolute URL
            URI requestHostURI

            if (request) {
                String host = httpHostResolver.resolve(request)
                requestHostURI = new URI(host)
            } else {
                log.warn("Constructed base URI from server configuration")
                requestHostURI = UriBuilder.of("/")
                    .scheme(sslConfig.isEnabled() ? 'https' : 'http')
                    .host(httpServerConfiguration.host.present ? httpServerConfiguration.host.get() : 'localhost')
                    .port(httpServerConfiguration.port.present ? httpServerConfiguration.port.get() : sslConfig.isEnabled() ? 443 : 80)
                    .build()
            }

            String contextPath = serverContextPathProvider.getContextPath() ?: ""

            mauroConfiguration.oauths.forEach {MauroConfiguration.OAuthConfig oAuthConfig ->

                final String providerName = oAuthConfig.oauthProvider

                // Get the login URI
                URI loginURI = oauthRouteUrlBuilder.buildLoginUri(providerName)
                String loginPath = loginURI.isAbsolute() ? loginURI.toString() : UriBuilder.of("/")
                    .scheme(requestHostURI.scheme)
                    .host(requestHostURI.host)
                    .port(requestHostURI.port)
                    .path(loginURI.getPath())
                    .build()
                    .toString()

                // A helpful warning
                if (contextPath && contextPath != '/' && !loginURI.getPath().startsWith(contextPath)) {

                    String meantLoginPath = loginURI.isAbsolute() ? loginURI.toString() : UriBuilder.of("/")
                        .path(contextPath)
                        .path(loginURI.getPath())
                        .build()
                        .toString()

                    URI callbackURI = oauthRouteUrlBuilder.buildCallbackUri(providerName)

                    String meantCallbackPath = loginURI.isAbsolute() ? callbackURI.toString() : UriBuilder.of("/")
                        .path(contextPath)
                        .path(callbackURI.getPath())
                        .build()
                        .toString()

                    log.warn("OAUTH: the login template at micronaut.security.oauth2.login-uri does not start with context-path at micronaut.server.context-path")
                    log.info("Perhaps you meant:")
                    log.info("OAUTH: micronaut.security.oauth2.login-uri = ${meantLoginPath}")
                    log.info("OAUTH: micronaut.security.oauth2.callback-uri = ${meantCallbackPath}")
                }

                OpenidConnectProvider openidConnectProvider = new OpenidConnectProvider(
                    null, oAuthConfig.appLabel, true, loginPath,
                    oAuthConfig.appImageUrl)
                openidConnectProviders << openidConnectProvider
            }
            return openidConnectProviders

        }

        return []
    }

}
