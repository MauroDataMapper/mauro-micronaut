package uk.ac.ox.softeng.mauro.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

@CompileStatic
@Singleton
@Slf4j
class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>> {

    @Override
    Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest,
                                                   AuthenticationRequest<?, ?> authenticationRequest) {
        log.debug('AuthenticationProvider :: authenticate')
        Flux.create(emitter -> {
            if (authenticationRequest.secret == "password") {
                emitter.next(AuthenticationResponse.success((String) authenticationRequest.identity, [id: authenticationRequest.identity] as Map<String, Object>))
                emitter.complete()
            } else {
                emitter.error(AuthenticationResponse.exception())
            }
        }, FluxSink.OverflowStrategy.ERROR) as Publisher<AuthenticationResponse>
    }
}

    @Override
