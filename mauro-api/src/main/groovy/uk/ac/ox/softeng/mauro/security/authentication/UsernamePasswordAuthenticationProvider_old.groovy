//package uk.ac.ox.softeng.mauro.security.authentication
//
//import groovy.transform.CompileStatic
//import groovy.util.logging.Slf4j
//import io.micronaut.http.HttpRequest
//import io.micronaut.security.authentication.AuthenticationProvider
//import jakarta.inject.Inject
//import jakarta.inject.Singleton
//import uk.ac.ox.softeng.mauro.persistence.security.CatalogueUserRepository
//
//@CompileStatic
//@Singleton
//@Slf4j
//class UsernamePasswordAuthenticationProvider_old implements AuthenticationProvider<HttpRequest<?>> {
//
//    @Inject
//    CatalogueUserRepository catalogueUserRepository
//
////    @Override
////    Publisher<AuthenticationResponse> authenticate_old(@Nullable HttpRequest<?> httpRequest,
////                                                       AuthenticationRequest<?, ?> authenticationRequest) {
////        log.debug('AuthenticationProvider :: authenticate')
////        Flux.create(emitter -> {
////            if (authenticationRequest.secret == "password") {
////                emitter.next(AuthenticationResponse.success((String) authenticationRequest.identity, [id: authenticationRequest.identity] as Map<String, Object>))
////                emitter.complete()
////            } else {
////                emitter.error(AuthenticationResponse.exception())
////            }
////        }, FluxSink.OverflowStrategy.ERROR) as Publisher<AuthenticationResponse>
////    }
//
////    @Override
////    Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
////        String emailAddress = (String) authenticationRequest.identity
////        catalogueUserRepository.readByEmailAddress(emailAddress).map {CatalogueUser catalogueUser ->
////            String password = (String) authenticationRequest.secret
////            Boolean valid = new String(catalogueUser.password, 'UTF-8') == SecurityUtils.saltPassword(password, catalogueUser.salt)
////            if (valid) {
////                log.info "Authentication successful for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
////                return AuthenticationResponse.success(emailAddress, [id: catalogueUser.id] as Map<String, Object>)
////            } else {
////                log.info "Authentication failed for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
////                throw AuthenticationResponse.exception(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
////            }
////        }
////    }
//}