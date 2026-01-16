package org.maurodata.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Value
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.config.AuthenticationModeConfiguration
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdAuthenticationMapper
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.security.utils.SecureRandomStringGenerator

@CompileStatic
@Singleton
@Slf4j
@Replaces(DefaultOpenIdAuthenticationMapper)
class MauroOpenIdAuthenticationMapper extends DefaultOpenIdAuthenticationMapper {

    @Value('${mauro.oauth.create-user:false}')
    boolean createUser

    @Value('${mauro.oauth.require-verified-email:true}')
    boolean requireVerifiedEmail

    @Property(name = "mauro.oauth.token-custom-validation")
    Map<String, String> tokenCustomValidation

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserCacheableRepository

    MauroOpenIdAuthenticationMapper(OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration, AuthenticationModeConfiguration authenticationModeConfiguration) {
        super(openIdAdditionalClaimsConfiguration, authenticationModeConfiguration)
    }

    @Override
    @Transactional
    Map<String, Object> buildAttributes(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Map<String, Object> claims = super.buildAttributes(providerName, tokenResponse, openIdClaims)
        if (!claims.email) authenticationException("Attempt to login with no  email address specified!")
        if (requireVerifiedEmail && !claims.email_verified) authenticationException("Attempt to login with unverified email address! [${claims.email}]") // Entra does not provide email_verified

        tokenCustomValidation.each { expectedKey, expectedValue ->
            def tokenValue = claims[expectedKey]

            boolean invalid =
                    tokenValue == null ||
                    (tokenValue instanceof String && tokenValue != expectedValue) ||
                    (tokenValue instanceof Collection<?> && !((Collection<?>)tokenValue).contains(expectedValue))

            if (invalid) {
                log.info("Attempt to login with missing claim! [expecting '${expectedValue}' in JWT token claim '${expectedKey}']")
                authenticationException("Please contact your system administrator for access")
            }
        }

        CatalogueUser user = catalogueUserCacheableRepository.readByEmailAddress((String) claims.email) ?: createUser(claims)
        if (!user) authenticationException("User does not exist for $claims.email")
        claims.id = user.id
        log.debug("claims: id: {}", user.id)
        claims
    }

    CatalogueUser createUser(Map<String, Object> claims) {

        if (createUser) {
            log.debug("User email address not found, adding new Catalogue user for : {}", claims.email)
            CatalogueUser newUser = new CatalogueUser().tap {
                pending = false
                disabled = false
                creationMethod = 'OpenID-Connect'
                tempPassword = null
                password = null
                firstName = claims.given_name
                lastName = claims.family_name
                emailAddress = claims.email
                salt = SecureRandomStringGenerator.generateSalt()
            }
            return catalogueUserCacheableRepository.save(newUser)
        }
        return null
    }

    static void authenticationException(String message) {
        throw new AuthenticationException(message)
    }
}
