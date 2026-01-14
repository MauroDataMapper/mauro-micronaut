package org.maurodata.security.authentication

import org.maurodata.service.authentication.UsernamePasswordService

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

@CompileStatic
@Singleton
@Slf4j
class UsernamePasswordAuthenticationProvider<B> implements HttpRequestAuthenticationProvider<B> {

    @Inject UsernamePasswordService usernamePasswordService

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserRepository

    @Override
    AuthenticationResponse authenticate(@Nullable HttpRequest<B> requestContext, @NonNull AuthenticationRequest<String, String> authRequest) {
        //#CWE-749
        if (!usernamePasswordService.micronautSecurityEndpointsLoginEnabled) {
            log.warn("Local login is not enabled")
            usernamePasswordService.failWait()
            return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
        }
        try {
            String emailAddress = authRequest.identity
            String password = authRequest.secret
            // #CWE-20, #CWE-1284
            final UsernamePasswordService.InputValidationCheckStatus checkStatus = usernamePasswordService.inputValidationCheckUsernamePassword(emailAddress, password)
            if (checkStatus != UsernamePasswordService.InputValidationCheckStatus.OK) {
                // #CWE-307
                usernamePasswordService.failWait()
                log.warn("Username / password failed input validation checks")
                return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
            }

            CatalogueUser catalogueUser = catalogueUserRepository.readByEmailAddress(emailAddress)
            boolean valid = false
            if (catalogueUser) {
                if (catalogueUser.tempPassword == password) {
                    final UsernamePasswordService.PasswordRequirementsCheckStatus checkPasswordStatus = usernamePasswordService.passwordRequirementsCheck(emailAddress, password, UsernamePasswordService.PasswordUse.TEMPORARY)
                    // #CWE-521
                    if (checkPasswordStatus != UsernamePasswordService.PasswordRequirementsCheckStatus.OK) {
                        log.warn("Temp password failed requirements checks: user [$catalogueUser.id] with email [$catalogueUser.emailAddress]")
                        usernamePasswordService.failWait()
                        throw AuthenticationResponse.exception(AuthenticationFailureReason.ACCOUNT_LOCKED)
                    }
                    valid = true
                } else if (usernamePasswordService.passwordEquals(catalogueUser.password, catalogueUser.salt, password)) {
                    // #CWE-521
                    final UsernamePasswordService.PasswordRequirementsCheckStatus checkPasswordStatus = usernamePasswordService.passwordRequirementsCheck(emailAddress, password, UsernamePasswordService.PasswordUse.MAIN)

                    if (checkPasswordStatus != UsernamePasswordService.PasswordRequirementsCheckStatus.OK) {
                        log.warn("Password failed requirements checks: user [$catalogueUser.id] with email [$catalogueUser.emailAddress]")
                        if (usernamePasswordService.mauroLoginPasswordLengthDenies) {
                            log.info("Denying entry: user [$catalogueUser.id] with email [$catalogueUser.emailAddress]")
                            usernamePasswordService.failWait()
                            throw AuthenticationResponse.exception(AuthenticationFailureReason.ACCOUNT_LOCKED)
                        } else {
                            log.info("To reset password: user [$catalogueUser.id] with email [$catalogueUser.emailAddress]")
                            if (!catalogueUser.resetToken) {
                                catalogueUser.resetToken = UUID.randomUUID()
                                catalogueUserRepository.update(catalogueUser)
                            }
                        }
                    }
                    valid = true
                }
            }

            // #CWE-307
            // Rate limit all password tries
            final long waited = usernamePasswordService.retryWait()
            if(waited > 0) {
                log.warn("Rate limiting: waited ${waited}ms")
            }

            if (valid) {
                log.info "Authentication successful for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                return AuthenticationResponse.success(emailAddress, [id: catalogueUser.id] as Map<String, Object>)
            } else {
                log.info "Authentication failed for user [$catalogueUser.id] with email [$catalogueUser.emailAddress]"
                // #CWE-307
                usernamePasswordService.failWait()
                return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
            }
        } catch (Exception exception) {
            // log exceptions here otherwise they are ignored
            log.error "Authentication failed due to exception [$exception]"
            // #CWE-307
            usernamePasswordService.failWait()
            return AuthenticationResponse.failure(AuthenticationFailureReason.UNKNOWN)
        }
    }
}
