package uk.ac.ox.softeng.mauro.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.authentication.AuthenticationFailureReason
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.security.CatalogueUserRepository
import uk.ac.ox.softeng.mauro.security.utils.SecurityUtils

@CompileStatic
@Singleton
@Slf4j
class UsernamePasswordAuthenticationProvider implements AuthenticationProvider<HttpRequest<?>> {

    @Inject
    CatalogueUserRepository catalogueUserRepository

//    @Override
//    Publisher<AuthenticationResponse> authenticate_old(@Nullable HttpRequest<?> httpRequest,
//                                                       AuthenticationRequest<?, ?> authenticationRequest) {
//        log.debug('AuthenticationProvider :: authenticate')
//        Flux.create(emitter -> {
//            if (authenticationRequest.secret == "password") {
//                emitter.next(AuthenticationResponse.success((String) authenticationRequest.identity, [id: authenticationRequest.identity] as Map<String, Object>))
//                emitter.complete()
//            } else {
//                emitter.error(AuthenticationResponse.exception())
//            }
//        }, FluxSink.OverflowStrategy.ERROR) as Publisher<AuthenticationResponse>
//    }

    @Override
    Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        String emailAddress = (String) authenticationRequest.identity
        catalogueUserRepository.readByEmailAddress(emailAddress).map {CatalogueUser catalogueUser ->
            String password = (String) authenticationRequest.secret
            Boolean valid = new String(catalogueUser.password, 'UTF-8') == SecurityUtils.saltPassword(password, catalogueUser.salt)
            if (valid) {
                log.info "Authentication successful for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                return AuthenticationResponse.success(emailAddress, [id: catalogueUser.id] as Map<String, Object>)
            } else {
                log.info "Authentication failed for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                throw AuthenticationResponse.exception(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
            }
        }
    }
}