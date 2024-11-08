package uk.ac.ox.softeng.mauro.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.convert.value.MutableConvertibleValues
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.config.RedirectService
import io.micronaut.security.filters.SecurityFilter
import io.micronaut.security.session.SessionLogoutHandler
import io.micronaut.session.Session
import io.micronaut.session.http.HttpSessionFilter
import jakarta.inject.Singleton

@Singleton
@Slf4j
@Replaces(SessionLogoutHandler)
@CompileStatic
class MauroSessionLogoutHandler extends SessionLogoutHandler {


    /**
     * Constructor.
     * @param redirectConfiguration Redirect Configuration
     * @param redirectService Redirection Service
     */
    MauroSessionLogoutHandler(RedirectConfiguration redirectConfiguration, RedirectService redirectService) {
        super(redirectConfiguration, redirectService)
    }

    public MutableHttpResponse<?> logout(HttpRequest<?> request) {
        removeAuthenticationFromSession(request);
        try {
             HttpResponse.ok()
        } catch (URISyntaxException e) {
            return HttpResponse.serverError();
        }
    }
    private void removeAuthenticationFromSession(HttpRequest<?> request) {
        MutableConvertibleValues<Object> attrs = request.getAttributes();
        Optional<Session> existing = attrs.get(HttpSessionFilter.SESSION_ATTRIBUTE, Session.class);
        if (existing.isPresent()) {
            Session session = existing.get();
            session.remove(SecurityFilter.AUTHENTICATION);
        }
    }
}
