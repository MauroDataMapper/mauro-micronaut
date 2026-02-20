package org.maurodata.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.cookie.SameSite
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.server.util.HttpHostResolver
import io.micronaut.security.oauth2.url.OauthRouteUrlBuilder
import jakarta.inject.Inject
import org.maurodata.controller.bootstrap.MauroConfiguration
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@CompileStatic
@Slf4j
@Requires(bean = OauthRouteUrlBuilder)
@Filter("/**")
class OAuthRedirectFilter implements HttpServerFilter {

    private Map<String, List<String>> loginPathToUiRedirectUrls = [:]

    static final String UI_REDIRECT_URL = "ui_redirect_url"

    @Inject
    HttpHostResolver httpHostResolver

    @Inject
    MauroConfiguration mauroConfiguration

    @Inject
    OauthRouteUrlBuilder oauthRouteUrlBuilder

    OAuthRedirectFilter() {
        if (mauroConfiguration && mauroConfiguration.oauths) {
            mauroConfiguration.oauths.forEach {MauroConfiguration.OAuthConfig oAuthConfig ->
                final String providerName = oAuthConfig.oauthProvider

                try {
                    // Get the login URI
                    URI loginURI = oauthRouteUrlBuilder.buildLoginUri(providerName)

                    // Map oauth login path -> list of acceptable ui_redirect_url
                    loginPathToUiRedirectUrls.put(loginURI.getPath(), oAuthConfig.uiRedirectUrls ?: [])

                } catch (Throwable th) {
                    log.warn("OAuth: Unable to apply ui_redirect_url check")
                    log.warn(th.getMessage())
                }
            }
        }

    }

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (loginPathToUiRedirectUrls.isEmpty()) {return chain.proceed(request)}
        final String requestPath = request.getPath()
        if (!loginPathToUiRedirectUrls.containsKey(requestPath)) {return chain.proceed(request)}

        final String redirectUri = request.getParameters().get("redirect_uri")
        if (redirectUri == null || redirectUri.trim().isEmpty()) {return chain.proceed(request)}

        final List<String> validRedirects = loginPathToUiRedirectUrls.get(requestPath)

        if (validRedirects == null) {
            log.error("OAuth: Internal error: somehow there is no list of valid redirects")
            return chain.proceed(request)
        }

        boolean isValidRedirectUri = validRedirects.isEmpty() || validRedirects.contains(redirectUri)

        String host = httpHostResolver.resolve(request)
        URI requestHostURI = new URI(host)

        boolean secure = requestHostURI.scheme == "https"

        // Use Mono.from to turn publisher -> Mono, then map the response
        return Mono.from(chain.proceed(request))
            .map {MutableHttpResponse<?> resp ->
                if (isValidRedirectUri) {
                    Cookie cookie = Cookie.of(UI_REDIRECT_URL, redirectUri)
                        .secure(secure)
                        .sameSite(secure ? SameSite.None : SameSite.Lax)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(300)
                    resp.cookie(cookie) // attaches cookie to the outgoing response
                }
                return resp
            } as Publisher<MutableHttpResponse<?>>
    }
}
