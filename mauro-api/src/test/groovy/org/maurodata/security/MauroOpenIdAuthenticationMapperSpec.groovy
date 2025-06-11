package org.maurodata.security

import com.nimbusds.jwt.JWTClaimsSet
import io.micronaut.context.annotation.Replaces
import io.micronaut.security.authentication.AuthenticationException
import io.micronaut.security.oauth2.endpoint.token.response.JWTOpenIdClaims
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse
import jakarta.inject.Inject
import spock.lang.Shared
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.authentication.MauroOpenIdAuthenticationMapper

@SecuredContainerizedTest
class MauroOpenIdAuthenticationMapperSpec extends SecuredIntegrationSpec {

    static String UNKNOWN_EMAIL = "unknownToMauroMicronaut@email.com"

    @Inject
    @Shared
    MauroOpenIdAuthenticationMapper mauroOpenIdAuthenticationMapper
    @Shared
    OpenIdTokenResponse openIdTokenResponseMock = new OpenIdTokenResponseMock()

    @Shared
    JWTOpenIdClaims openIdClaims


    void 'openid login with known email - address exists- claims attributes should return success'() {
        given:
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject("mauro one")
                .claim("email_verified", true)
                .claim("email", adminUser.emailAddress)

                .build()

        openIdClaims = new JWTOpenIdClaims(jwtClaimsSet)
        int noOfUsers = catalogueUserRepository.findAll().size()
        when:
        Map<String, Object> claims = mauroOpenIdAuthenticationMapper.buildAttributes("keycloak", openIdTokenResponseMock, openIdClaims)

        then:
        claims
        //nothing added to catalogueUserRepo
        catalogueUserRepository.findAll().size() == noOfUsers

    }


    void 'openid login with unknown email(user does not exist) - createUserFlag not set -should throw AuthenticationException'() {
        given:
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject("mauro one")
                .claim("email_verified", true)
                .claim("email", UNKNOWN_EMAIL)
                .claim("given_name", "first ")
                .claim("family_name", "admin ")
                .build()

        openIdClaims = new JWTOpenIdClaims(jwtClaimsSet)
        int noOfUsers = catalogueUserRepository.findAll().size()
        mauroOpenIdAuthenticationMapper.createUser = false

        when:
        mauroOpenIdAuthenticationMapper.buildAttributes("keycloak", openIdTokenResponseMock, openIdClaims)

        then:
        thrown(AuthenticationException)

        and:
        //nothing added to catalogueUser repo
        catalogueUserRepository.findAll().size() == noOfUsers

    }

    void 'openid login with unknown email(user does not exist) - createUserFlag  set -should create new CatalogueUser'() {
        given:
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject("mauro one")
                .claim("email_verified", true)
                .claim("email", UNKNOWN_EMAIL)
                .claim("given_name", "first ")
                .claim("family_name", "admin ")
                .build()

        openIdClaims = new JWTOpenIdClaims(jwtClaimsSet)
        mauroOpenIdAuthenticationMapper.createUser = true

        when:
        Map<String,Object> claims = mauroOpenIdAuthenticationMapper.buildAttributes("keycloak", openIdTokenResponseMock, openIdClaims)
        then:
        claims

        when:
        //new user added
        CatalogueUser added = catalogueUserRepository.readById(claims.id)

        then:
        added
    }

    @Replaces(OpenIdTokenResponse)
    class OpenIdTokenResponseMock extends OpenIdTokenResponse {
        String idToken = 'eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJQT0xQUDM5cHNvWF9NMW1jcWV4V2ZnMURzNVBWTE1sOEhyUk91dlRQNVhnIn0.eyJleHAiOjE3MzE0OTkwMzcsImlhdCI6MTczMTQ5ODk3NywiYXV0aF90aW1lIjoxNzMxNDk4OTcxLCJqdGkiOiI3YzFiYjZlNC1lYmU2LTRhMjMtOThlYi0yZTE2NDA3NGEyNWUiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwMDkvcmVhbG1zL21hc3RlciIsImF1ZCI6Im1hdXJvIiwic3ViIjoiNjdmYWFmNDgtMDI0Yi00MjYzLWIxYzYtNzhmZTFmZWI2NDhhIiwidHlwIjoiSUQiLCJhenAiOiJtYXVybyIsIm5vbmNlIjoiMTBlYmFhNmMtYWU1OC00Mzg2LTg1YjktODJiODQ0YmI4Yzg5Iiwic2Vzc2lvbl9zdGF0ZSI6IjM0YmYwOTYyLTQxZmItNGFkYy1iZDFhLWM0NmZkOTIxNTE2OCIsImF0X2hhc2giOiJjMWFfSV9nX014OVpDVkVMVFFtclZBIiwiYWNyIjoiMSIsInNpZCI6IjM0YmYwOTYyLTQxZmItNGFkYy1iZDFhLWM0NmZkOTIxNTE2OCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJuYW1lIjoiQWRtaW4gT25lIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWRtaW5fb25lIiwiZ2l2ZW5fbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJPbmUiLCJlbWFpbCI6ImFkbWluLm9uZUBzZGV0ZXN0LmNvbSJ9.GonTSLFjU_Tfev8zjSOWZ1e9_IM6XbnbqM55S3zbGVG4zwIScdRn_UwjhD5UvQVoV6tlAe1UHWIb_XEYlxsOJ_yMptaF9wY2xmkHr8NcRQGrPoVSH7zrJz62AUde6tM83XDAbYX1OOjM8p5xm6Zf2FeD3Q4DI4saZzMxv_9TNzmR8xYXvt-PdOdPjlai-2bj6m-zbe3EzhnqYFT_PuISBoaTkTYBu295qbUysJo6xoWMxDwgpWq4B2e0SX1u70RZ2q4PTDzU7kJYYMVSfDdQx8QhY91xfjjgKjz-8mRBH2lB3bGfZS0f7pOhEeUg3hqamgaHxNye_X6_aCs2BPMD7Q'
    }

}