package uk.ac.ox.softeng.mauro.security.authentication

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Value
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.config.AuthenticationModeConfiguration
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.security.utils.SecureRandomStringGenerator

@Singleton
@Slf4j
@Replaces(DefaultOpenIdAuthenticationMapper)
class MauroOpenIdAuthenticationMapper extends DefaultOpenIdAuthenticationMapper {

    @Value('${mauro.oauth.create-user:false}')
    boolean createUser

    @Inject
    ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserCacheableRepository

    MauroOpenIdAuthenticationMapper(OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration, AuthenticationModeConfiguration authenticationModeConfiguration) {
        super(openIdAdditionalClaimsConfiguration, authenticationModeConfiguration)
    }

    @Override
    @Transactional
    Map<String, Object> buildAttributes(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Map<String, Object> claims = super.buildAttributes(providerName, tokenResponse, openIdClaims)
        //in theory this code is unreachable from openid provider
        if (!claims.email) authenticationException("Attempt to login with no  email address specified!")

        if (!claims.email_verified) authenticationException("Attempt to login with unverified email address! [${claims.email}]")
        CatalogueUser user = catalogueUserCacheableRepository.readByEmailAddress((String) claims.email) ?: createUser(claims)
        if (!user) authenticationException("User does not exist for $claims.email")
        claims.id = user.id
        log.debug("claims: id: {}", user.id)
        claims
    }

    CatalogueUser createUser(Map<String, Object> claims) {
        CatalogueUser saved
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
            saved = catalogueUserCacheableRepository.save(newUser)
        }
        saved
    }

    static void authenticationException(String message) {
        throw new AuthenticationException(message)
    }
}
