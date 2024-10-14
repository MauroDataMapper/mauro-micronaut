package uk.ac.ox.softeng.mauro.security.authentication

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

import java.time.Duration
import java.time.format.DateTimeParseException

@Singleton
@Slf4j
@CompileStatic
class OpenidConnectAuthenticationService {


   @Value('${micronaut.security.oauth2.clients.keycloak.openid.token.url}') String tokenUrl


    @Transactional
    CatalogueUser authenticateAndObtainUser(Map<String, Object> authenticationInformation) {
        log.info('Attempt to authenticate system using Openid Connect, authenticationInformation:')
        authenticationInformation.each {
            log.info("key, {} ,value: {}", it.key, it.value)
        }
        log.warn(" not yet implemented")
        null
//        AuthorizationResponseParameters authorizationResponseParameters = new AuthorizationResponseParameters(authenticationInformation)
//        HttpSession session = authenticationInformation.session as HttpSession
//
//        OpenidConnectProvider openidConnectProvider = openidConnectProviderService.get(authorizationResponseParameters.openidConnectProviderId)
//
//        if (!openidConnectProvider) {
//            log.warn('Attempt to authenticate using unknown OAUTH Provider')
//            return null
//        }
//
//        log.trace('Requesting token\n{}', authorizationResponseParameters.toString(session.id, openidConnectProvider.label))
//        Map<String, Object> responseBody = openidConnectProviderService.loadTokenFromOpenidConnectProvider(openidConnectProvider,
//                openidConnectProvider.getAccessTokenRequestParameters(
//                        authorizationResponseParameters.code,
//                        authorizationResponseParameters.redirectUri,
//                        authorizationResponseParameters.sessionState)
//        )

//        if (!responseBody) {
//            log.warn("Failed to get access token from Openid Connect Provider [${openidConnectProvider.label}]")
//            return null
//        }

//        if (responseBody.error) {
//            log.warn("Failed to get access token from Openid Connect Provider [${openidConnectProvider.label}] because [${responseBody.error_description}]")
//            return null
//        }
//
//        OpenidConnectToken token = openidConnectTokenService.createToken(openidConnectProvider, responseBody, session.id)
//
//        log.debug('Verifying token for session {}', session.id)
//        if (!openidConnectTokenService.verifyIdToken(token, authorizationResponseParameters.sessionState)) {
//            return null
//        }
//
//        String emailAddress = token.getIdTokenClaim('email').asString()
//
//        CatalogueUser user = catalogueUserService.findByEmailAddress(emailAddress)
//
//        if (user?.isDisabled()) {
//            // User is not active, so return null
//            return null
//        }
//
//        Map<String, ApiProperty> apiPropertyMap = getRequiredApiProperties()
//        if (!user && !apiPropertyMap.autoRegisterUserProperty?.value?.toBoolean()) {
//            // User not found, so return null
//            return null
//        }
//
//        if (!user) {
//            log.info('Creating new user {}', emailAddress)
//
//            Map<String, Object> userInfoBody = openidConnectProviderService.loadUserInfoFromOpenidConnectProvider(openidConnectProvider, token.accessToken)
//
//            URL issuerUrl = openidConnectProvider.discoveryDocument.issuer.toURL()
//            user = catalogueUserService.createNewUser(emailAddress: emailAddress,
//                    password: null,
//                    firstName: userInfoBody.given_name ?: 'Unknown',
//                    lastName: userInfoBody.family_name ?: 'Unknown',
//                    //createdBy: "openidConnectAuthentication@${issuerUrl.host}",
//                    createdBy: "openidConnectAuthentication@maurodata.org",
//                    pending: false,
//                    creationMethod: 'OpenID-Connect')
//
//            if (!user.validate()) throw new ApiInvalidModelException('OCAS02:', 'Invalid user creation', user.errors)
//            user.save(flush: true, validate: false)
//            user.addCreatedEdit(user)
//        }
//
//        token.createdBy = user.emailAddress
//        token.catalogueUser = user
//
//        Duration timeoutOverride = null
//        try {
//            timeoutOverride = Duration.parse("pt${grailsApplication.config.maurodatamapper.openidConnect.session.timeout}")
//            log.debug('Overriding standard session timeout to {}', timeoutOverride)
//        } catch (DateTimeParseException ignored) {}
//
//        openidConnectTokenService.validateAndSave(token)
//        openidConnectAccessService.storeTokenDataIntoHttpSession(token,session , timeoutOverride)

 //       user
    }


    Map<String, Object> loadTokenFromOpenidConnectProvider(Map<String, String> requestBody) {
        log.debug('Loading token from OIC provider')
        URL tokenEndpoint = UriBuilder.of(tokenUrl).build().toURL()

        HttpClient client = HttpClient.create(getClientHostUrl(tokenEndpoint))
        HttpRequest request = HttpRequest.POST(tokenEndpoint.path, requestBody)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)

        loadMapFromEndpoint(client, request)
    }

    private Map<String, Object> loadMapFromEndpoint(HttpClient httpClient, HttpRequest httpRequest) {
        try {
            httpClient.toBlocking().exchange(httpRequest, Argument.mapOf(String, Object)).body()
        } catch (HttpClientResponseException e) {
            switch (e.status) {
                case HttpStatus.UNAUTHORIZED:
                case HttpStatus.FORBIDDEN:
                    return [:]
                default:
                    Map body = e.response.body() as Map<String, Object>
                    body.error ? body : [:]
            }
        }
    }
    private URL getClientHostUrl(URL fullUrl) {
        String clientHostUrl = "${fullUrl.protocol}://${fullUrl.host}"
        if (fullUrl.port != -1) clientHostUrl = "${clientHostUrl}:${fullUrl.port}"
        clientHostUrl.toURL()
    }
}
