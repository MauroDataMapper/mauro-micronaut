package org.maurodata.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationFailureReason
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.security.utils.SecurityUtils

@CompileStatic
@Singleton
@Slf4j
class UsernamePasswordAuthenticationProvider<B> implements HttpRequestAuthenticationProvider<B> {

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository

    @Override
    AuthenticationResponse authenticate(@Nullable HttpRequest<B> requestContext, @NonNull AuthenticationRequest<String, String> authRequest) {
        try {
            String emailAddress = authRequest.identity
            String password = authRequest.secret

            CatalogueUser catalogueUser = catalogueUserRepository.readByEmailAddress(emailAddress)
            boolean valid
            if (catalogueUser) {
                valid = (catalogueUser.tempPassword == password)
                valid = valid || (new String(catalogueUser.password, 'UTF-8') == SecurityUtils.saltPassword(password, catalogueUser.salt))
            }

            if (valid) {
                log.info "Authentication successful for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                return AuthenticationResponse.success(emailAddress, [id: catalogueUser.id] as Map<String, Object>)
            } else {
                log.info "Authentication failed for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
            }
        } catch (Exception exception) {
            // log exceptions here otherwise they are ignored
            log.error "Authentication failed due to exception [$exception]"
            return AuthenticationResponse.failure(AuthenticationFailureReason.UNKNOWN)
        }
    }
}
