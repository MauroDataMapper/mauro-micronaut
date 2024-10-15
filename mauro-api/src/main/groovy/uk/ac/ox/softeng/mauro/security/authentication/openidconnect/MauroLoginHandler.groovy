package uk.ac.ox.softeng.mauro.security.authentication.openidconnect

import groovy.transform.CompileStatic
import io.micronaut.security.oauth2.endpoint.token.response.IdTokenLoginHandler
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.security.authentication.MauroSessionLoginHandler
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class MauroLoginHandler {
    @Inject @IdToken IdTokenLoginHandler idTokenLoginHandler
    @Inject @Session MauroSessionLoginHandler mauroSessionLoginHandler

}
