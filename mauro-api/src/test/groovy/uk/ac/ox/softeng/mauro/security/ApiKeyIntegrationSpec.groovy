package uk.ac.ox.softeng.mauro.security

import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.ApiKey
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared

@SecuredContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-api-key.sql", phase = Sql.Phase.AFTER_EACH)
class ApiKeyIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID apiPropertyId

    void 'create an api key as a user'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        then:
        response.id
        !response.disabled
        response.catalogueUserId == user.id
        response.name == "Test Key"
        !response.expired

        when:
        UUID newApiKeyId = response.id
        ListResponse<ApiKey> apiKeys = GET("/catalogueUsers/$user.id/apiKeys", ListResponse, ApiKey)

        then:
        apiKeys.count == 1
        apiKeys.items.find {it.name == "Test Key"}

        when:
        PUT("/catalogueUsers/$user.id/apiKeys/$newApiKeyId/disable", [])

        response = GET("/catalogueUsers/$user.id/apiKeys/$newApiKeyId", ApiKey)
        then:
        response.disabled

        when:
        PUT("/catalogueUsers/$user.id/apiKeys/$newApiKeyId/enable", [])

        response = GET("/catalogueUsers/$user.id/apiKeys/$newApiKeyId", ApiKey)
        then:
        !response.disabled

    }

    void 'create an api key as an administrator'() {
        given:
        loginAdmin()

        when:
        ApiKey response = POST("/catalogueUsers/$adminUser.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        then:
        response.id
        !response.disabled
        response.catalogueUserId == adminUser.id
        response.name == "Test Key"
        !response.expired

        when:
        ListResponse<ApiKey> apiKeys = GET("/catalogueUsers/$adminUser.id/apiKeys", ListResponse, ApiKey)

        then:
        apiKeys.count == 1
        apiKeys.items.find {it.name == "Test Key"}

    }


    void 'users cannot create an api key for another user'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$adminUser.id/apiKeys", [
            name         : "Test Key For Another User",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        then: 'the create endpoint returns forbidden'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

    }

    void 'administrators can create an api key for another user'() {
        given:
        loginAdmin()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name:  'Test Key For Another User',
            expiresInDays: 10,
            refreshable: true
        ], ApiKey)

        then:
        response.id
        !response.disabled
        response.catalogueUserId == user.id
        response.name == 'Test Key For Another User'
        !response.expired

        when:
        ListResponse<ApiKey> apiKeys = GET("/catalogueUsers/$user.id/apiKeys", ListResponse, ApiKey)

        then:
        apiKeys.count == 1
        apiKeys.items.find {it.name == 'Test Key For Another User'}

        when:
        apiKeys = GET("/catalogueUsers/$adminUser.id/apiKeys", ListResponse, ApiKey)

        then: 'But this hasnt created it for the admin user themselves'
        apiKeys.count == 0


    }

    void 'Test api key works for user'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        String apiKeyId = response.id

        logout()

        // Check a couple of endpoints are unavailable after logging out

        GET("admin/emails")
        then:
        def e = thrown(Exception)
        e.message == "Unauthorized"

        when:
        GET("catalogueUsers/currentUser")

        then:
        e = thrown(Exception)
        e.message == "User is not authenticated"

        when:
        apiKey = UUID.fromString(apiKeyId)
        CatalogueUser userResponse = (CatalogueUser) GET("catalogueUsers/currentUser", CatalogueUser)

        then:
        userResponse.id == user.id

        when: // But we still can't get the email list which requires an administrator
        ListResponse<Email> emailsResponse = (ListResponse<Email>) GET("admin/emails", ListResponse, Email)

        then:
        e = thrown(Exception)
        e.message == "Forbidden"

        when: // when we remove the api key
        apiKey = null
        GET("catalogueUsers/currentUser")

        then: // we're back to being unauthenticated
        e = thrown(Exception)
        e.message == "User is not authenticated"
    }

    void 'Test api key works for administrator'() {
        given:
        loginAdmin()

        when:
        ApiKey response = POST("/catalogueUsers/$adminUser.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        String apiKeyId = response.id

        logout()

        // Check a couple of endpoints are unavailable after logging out

        GET("admin/emails")
        then:
        def e = thrown(Exception)
        e.message == "Unauthorized"

        when:
        GET("catalogueUsers/currentUser")

        then:
        e = thrown(Exception)
        e.message == "User is not authenticated"

        when:
        apiKey = UUID.fromString(apiKeyId)
        CatalogueUser userResponse = (CatalogueUser) GET("catalogueUsers/currentUser", CatalogueUser)

        then:
        userResponse.id == adminUser.id

        when: // But we still can't get the email list which requires an administrator
        ListResponse<Email> emailsResponse = (ListResponse<Email>) GET("admin/emails", ListResponse, Email)

        then:
        emailsResponse.count == 0

        when: // when we remove the api key
        apiKey = null
        GET("catalogueUsers/currentUser")

        then: // we're back to being unauthenticated
        e = thrown(Exception)
        e.message == "User is not authenticated"
    }


    void 'Test random api key doesnt work'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true
        ], ApiKey)

        String apiKeyId = response.id

        logout()

        // Check a couple of endpoints are unavailable after logging out

        GET("catalogueUsers/currentUser")

        then:
        def e = thrown(Exception)
        e.message == "User is not authenticated"

        when:
        apiKey = UUID.randomUUID()
        CatalogueUser userResponse = (CatalogueUser) GET("catalogueUsers/currentUser", CatalogueUser)

        then:
        e = thrown(Exception)
        e.message == "User is not authenticated"
    }

    void 'Test disabled api key doesnt work'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: 10,
            refreshable  : true,
            disabled     : true
        ], ApiKey)

        String apiKeyId = response.id

        logout()

        // Check a couple of endpoints are unavailable after logging out

        GET("catalogueUsers/currentUser")

        then:
        def e = thrown(Exception)
        e.message == "User is not authenticated"

        when:
        apiKey = UUID.randomUUID()
        CatalogueUser userResponse = (CatalogueUser) GET("catalogueUsers/currentUser", CatalogueUser)

        then:
        e = thrown(Exception)
        e.message == "User is not authenticated"
    }

    void 'Test expired api key doesnt work'() {
        given:
        loginUser()

        when:
        ApiKey response = POST("/catalogueUsers/$user.id/apiKeys", [
            name         : "Test Key",
            expiresInDays: -1,
            refreshable  : true,
            disabled     : true
        ], ApiKey)

        String apiKeyId = response.id

        logout()

        // Check a couple of endpoints are unavailable after logging out

        GET("catalogueUsers/currentUser")

        then:
        def e = thrown(Exception)
        e.message == "User is not authenticated"

        when:
        apiKey = UUID.randomUUID()
        CatalogueUser userResponse = (CatalogueUser) GET("catalogueUsers/currentUser", CatalogueUser)

        then:
        e = thrown(Exception)
        e.message == "User is not authenticated"
    }

}