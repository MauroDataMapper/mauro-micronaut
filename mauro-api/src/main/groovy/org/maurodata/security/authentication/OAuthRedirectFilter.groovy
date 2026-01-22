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
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@Slf4j
@Filter("/oauth/login/**")
class OAuthRedirectFilter implements HttpServerFilter {

    static final String UI_REDIRECT_URL = "ui_redirect_url"

    @Inject HttpHostResolver httpHostResolver


    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain ) { // (1)
        String redirectUri = null
        redirectUri = request.getParameters().get("redirect_uri")

        String host = httpHostResolver.resolve(request)
        URI requestHostURI = new URI(host)
        requestHostURI.scheme

        // Use Mono.from to turn publisher -> Mono, then map the response
        return Mono.from(chain.proceed(request))
            .map { MutableHttpResponse<?> resp ->
                if(redirectUri) {
                    Cookie cookie = Cookie.of(UI_REDIRECT_URL, redirectUri)
                        .secure(requestHostURI.scheme == "https")
                        .sameSite(SameSite.None)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(300)
                    resp.cookie(cookie) // attaches cookie to the outgoing response
                }
                return resp
            } as Publisher<MutableHttpResponse<?>>
    }
}
