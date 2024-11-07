package uk.ac.ox.softeng.mauro.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.util.functional.ThrowingSupplier
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.filters.SecurityFilter
import io.micronaut.security.session.SessionLoginHandler
import io.micronaut.session.Session
import io.micronaut.session.SessionStore
import io.micronaut.session.http.SessionForRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.CatalogueUserCacheableRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

@Singleton
@Slf4j
@Replaces(SessionLoginHandler)
class MauroSessionLoginHandler extends SessionLoginHandler {

    @Inject
    AccessControlService accessControlService

    @Inject
    CatalogueUserCacheableRepository catalogueUserCacheableRepository

    PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence

    MauroSessionLoginHandler(RedirectConfiguration redirectConfiguration, SessionStore<Session> sessionStore, @Nullable PriorToLoginPersistence<HttpRequest<?>, MutableHttpResponse<?>> priorToLoginPersistence, RedirectService redirectService) {
        super(redirectConfiguration, sessionStore, priorToLoginPersistence, redirectService)
        this.priorToLoginPersistence = priorToLoginPersistence
    }

    @Override
    MutableHttpResponse<?> loginSuccess(Authentication authentication, HttpRequest<?> request) {
        if (request.path.contains("/authentication/login")) {
            MutableHttpResponse defaultResponse = super.loginSuccess(authentication, request)
            //if (defaultResponse.status == HttpStatus.OK || loginSuccess == '/') {
                return HttpResponse.ok(catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id))
           // } else {
          //      defaultResponse
           // }
        } else {
            saveAuthenticationInSession(authentication, request)
            //     if (loginSuccess == null || loginSuccess == '/') {
            try {
                CatalogueUser user = catalogueUserCacheableRepository.readById((UUID) authentication.attributes.id)
                ShortenedCatalogueUser shortenedCatalogueUser = new ShortenedCatalogueUser(user.id, user.emailAddress, user.firstName,
                        user.lastName, user.pending, user.disabled)
                if (user.createdBy) shortenedCatalogueUser.createdBy = user.createdBy
                else shortenedCatalogueUser.createdBy = user.emailAddress
                return HttpResponse.ok(shortenedCatalogueUser)
//        }
//        try {
//            MutableHttpResponse<?> response = HttpResponse.status(HttpStatus.SEE_OTHER);
//            ThrowingSupplier<URI, URISyntaxException> uriSupplier = loginSuccessUriSupplier(loginSuccess, request, response)
//            URI location = uriSupplier.get()
//            response.header('location', location.toString())
//          //  response.getHeaders().location(loginSuccessUriSupplier(loginSuccess, request, response).get());
//            response;
            } catch (URISyntaxException e) {
                return HttpResponse.serverError();
            }
        }
    }

    @Override
    MutableHttpResponse<?> loginFailed(AuthenticationResponse authenticationFailed, HttpRequest<?> request) {
        MutableHttpResponse defaultResponse = super.loginFailed(authenticationFailed, request)
        if (defaultResponse.status == HttpStatus.OK) {
            log.debug 'Login failed'
            return HttpResponse.unauthorized()
        } else {
            defaultResponse
        }
    }

    @NonNull
    protected ThrowingSupplier<URI, URISyntaxException> loginSuccessUriSupplier(@NonNull String loginSuccess,
                                                                                HttpRequest<?> request,
                                                                                @NonNull MutableHttpResponse<?> response) {
        ThrowingSupplier<URI, URISyntaxException> uriSupplier = () -> new URI(loginSuccess);
        if (priorToLoginPersistence != null) {
            Optional<URI> originalUri = priorToLoginPersistence.getOriginalUri(request, response);
            if (originalUri.isPresent()) {
                uriSupplier = originalUri::get;
            }
        }
        return uriSupplier;
    }

    protected void saveAuthenticationInSession(Authentication authentication, HttpRequest<?> request) {
        Session session = SessionForRequest.find(request).orElseGet(() -> SessionForRequest.create(sessionStore, request));
        session.put(SecurityFilter.AUTHENTICATION, authentication);
    }


    class ShortenedCatalogueUser {
        UUID uuid
        String emailAddress
        String firstName
        String lastName
        Boolean pending
        Boolean disabled

        String createdBy

        ShortenedCatalogueUser(UUID uuid, String emailAddress, String firstName,
                               String lastName, Boolean pending, Boolean disabled) {
            this.uuid = uuid
            this.emailAddress = emailAddress
            this.firstName = firstName
            this.lastName = lastName
            this.pending = pending
            this.disabled = disabled

        }


    }
}
