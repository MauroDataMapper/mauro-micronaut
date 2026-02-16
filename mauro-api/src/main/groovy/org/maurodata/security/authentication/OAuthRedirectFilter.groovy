package org.maurodata.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.cookie.SameSite
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.server.util.HttpHostResolver
import jakarta.inject.Inject
import org.maurodata.controller.bootstrap.MauroConfiguration
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@Slf4j
@Filter("/oauth/login/**")
class OAuthRedirectFilter implements HttpServerFilter {

    static final String UI_REDIRECT_URL = "ui_redirect_url"

    @Inject HttpHostResolver httpHostResolver

    @Inject
    MauroConfiguration mauroConfiguration

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain ) { // (1)
        String redirectUri = null
        redirectUri = request.getParameters().get("redirect_uri")

        String oauthProvider = request.getPath().replace("/oauth/login/", "")
        String validUIRedirects = mauroConfiguration.oauths.find {
            it.oauthProvider == oauthProvider
        }?.uiRedirectUrls

        // If no valid redirects are set, resort to the usual behaviour
        if(!validUIRedirects) {
            validUIRedirects = []
        }

        boolean isValidRedirectUri = redirectUri && validUIRedirects.contains(redirectUri)

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
