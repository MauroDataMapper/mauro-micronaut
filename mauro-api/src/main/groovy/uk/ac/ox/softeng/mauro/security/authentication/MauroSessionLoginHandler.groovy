package uk.ac.ox.softeng.mauro.security.authentication

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTParser
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.type.Headers
import io.micronaut.core.util.functional.ThrowingSupplier
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.netty.NettyHttpHeaders
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationMode
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.config.SecurityConfigurationProperties
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.errors.ObtainingAuthorizationErrorCode
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdAuthenticationMapper
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.security.token.cookie.AccessTokenCookieConfiguration
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

import java.text.ParseException
import java.time.Duration

@Singleton
@Slf4j
@Replaces(SessionLoginHandler)
class MauroSessionLoginHandler extends SessionLoginHandler {

    @Inject
    AccessControlService accessControlService

    @Inject
    CatalogueUserCacheableRepository catalogueUserCacheableRepository

    protected final AccessTokenCookieConfiguration accessTokenCookieConfiguration;
    protected final PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence;

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore, @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService)
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        log.debug('request path: {}', request.path)
        if (request.path.contains('/authentication')) {
            saveAuthenticationInSession(authentication, request)
            MutableHttpResponse defaultResponse = loginSuccessResponse(request)
            if (defaultResponse.status == HttpStatus.OK) {
                log.debug 'Successful login, returning Authentication'
                return HttpResponse.ok(catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id))
            } else {
                defaultResponse
            }
        } else {
            applyCookies(createSuccessResponse(request), getCookies(authentication, request));
        }
    }

    MutableHttpResponse<?> createSuccessResponse(HttpRequest<?> request) {
        try {
            if (loginSuccess == null) {
                return HttpResponse.ok();
            }
            MutableHttpResponse<?> response = HttpResponse.status(HttpStatus.SEE_OTHER);
            ThrowingSupplier<URI, URISyntaxException> uriSupplier = () -> new URI(loginSuccess);
            if (priorToLoginPersistence != null) {
                Optional<URI> originalUri = priorToLoginPersistence.getOriginalUri(request, response);
                if (originalUri.isPresent()) {
                    uriSupplier = originalUri::get;
                }
            }
         //   response.getHeaders().location(uriSupplier.get());
            NettyHttpHeaders headers = response.getHeaders()
            headers.location(uriSupplier.get())
            return response;
        } catch (URISyntaxException e) {
            return HttpResponse.serverError();
        }
    }


    @Override
    MutableHttpResponse<?> loginFailed(AuthenticationResponse authenticationFailed, HttpRequest<?> request) {
        log.debug('request path: {}', request.path)
        if (request.path.contains('/authentication')) {
            MutableHttpResponse defaultResponse = super.loginFailed(authenticationFailed, request)
            if (defaultResponse.status == HttpStatus.OK) {
                log.debug 'Login failed'
                return HttpResponse.unauthorized()
            } else {
                defaultResponse
            }
        } else null
    }

    protected MutableHttpResponse<?> applyCookies(MutableHttpResponse<?> response, List<Cookie> cookies) {
        log.debug("apply cookies")
        for (Cookie cookie : cookies) {
            log.debug("cookie: {}", cookie.toString())
            response = response.cookie(cookie);
        }
    }

    List<Cookie> getCookies(Authentication authentication, HttpRequest<?> request) {
        List<Cookie> cookies = new ArrayList<>(1);
        String accessToken = parseIdToken(authentication).orElseThrow(() -> new OauthErrorResponseException(ObtainingAuthorizationErrorCode.SERVER_ERROR, "Cannot obtain an access token", null));

        Cookie jwtCookie = Cookie.of(accessTokenCookieConfiguration.getCookieName(), accessToken);
        jwtCookie.configure(accessTokenCookieConfiguration, request.isSecure());
        jwtCookie.maxAge(cookieExpiration(authentication, request));
        cookies.add(jwtCookie);
        cookies
    }

    Optional<String> parseIdToken(Authentication authentication) {
        Map<String, Object> attributes = authentication.getAttributes();
        if (!attributes.containsKey(OpenIdAuthenticationMapper.OPENID_TOKEN_KEY)) {

            log.warn("{} should be present in user details attributes to use {}:{}", OpenIdAuthenticationMapper.OPENID_TOKEN_KEY, SecurityConfigurationProperties.PREFIX + ".authentication", AuthenticationMode.IDTOKEN);

            return Optional.empty();
        }
        Object idTokenObjet = attributes.get(OpenIdAuthenticationMapper.OPENID_TOKEN_KEY);
        if (!(idTokenObjet instanceof String)) {
            log.warn("{} present in user details attributes should be of type String to use {}:{}", OpenIdAuthenticationMapper.OPENID_TOKEN_KEY, SecurityConfigurationProperties.PREFIX + ".authentication", AuthenticationMode.IDTOKEN);
            return Optional.empty();
        }
        return Optional.of((String) idTokenObjet);
    }
    Duration cookieExpiration(Authentication authentication, HttpRequest<?> request) {
        Optional<String> idTokenOptional = parseIdToken(authentication);
        if (!idTokenOptional.isPresent()) {
            return Duration.ofSeconds(0);
        }
        String idToken = idTokenOptional.get();
        try {
            JWT jwt = JWTParser.parse(idToken);
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            if (exp == null) {
                log.warn("unable to define a cookie expiration because id token exp claim is not set");
                return Duration.ofSeconds(0);
            }
            return Duration.between(new Date().toInstant(), exp.toInstant());
        } catch (ParseException e) {
            log.warn("unable to define a cookie expiration because id token cannot be parsed to JWT");
        }
        return Duration.ofSeconds(0);
    }

}
