package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Unroll

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-subscribed-catalogue.sql"], phase = Sql.Phase.AFTER_EACH)
class SubscribedCatalogueIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application
    @Inject
    ObjectMapper mapper

    void cleanup() {
        logout()
    }

    void 'logged in and admin user - can access admin subscribedCatalogues'() {
        given:
        loginAdmin()

        when:
        Map<String, Object> getAllResp = GET(ADMIN_SUBSCRIBED_CATALOGUES_PATH)

        then:
        getAllResp
        getAllResp.count == 0
        logout()

        loginUser()
        when:
        getAllResp = GET(ADMIN_SUBSCRIBED_CATALOGUES_PATH)

        then:
        getAllResp
        getAllResp.count == 0
    }

    void 'user not signed in - cannot access admin subscribedCatalogues endpoint'() {
        when:
        GET(ADMIN_SUBSCRIBED_CATALOGUES_PATH, ListResponse<SubscribedCatalogue>)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'adminUser can create SubscribedCatalogue'() {
        given:
        loginAdmin()

        when:
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

        then:
        subscribedCatalogue
        subscribedCatalogue.url == "https://maurosandbox.com/sandbox"
        subscribedCatalogue.subscribedCatalogueAuthenticationType == SubscribedCatalogueAuthenticationType.API_KEY
        subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MAURO_JSON
        subscribedCatalogue.connectTimeout == 30
    }

    @Unroll
    void '#currentUser cannot create SubscribedCatalogue -result is #exceptionStatus'() {
        given:
        currentUser

        when:
        POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == exceptionStatus

        where:
        currentUser | exceptionStatus
        loginUser() | HttpStatus.FORBIDDEN
        null        | HttpStatus.UNAUTHORIZED
    }

    void 'admin user - can test subscribedCatalogue connection'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

        when:
        HttpStatus httpStatus = GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        httpStatus == HttpStatus.OK
    }


    void 'non adminUsers test subscribedCatalogue connection - should return exception'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)
        logout()
        loginUser()

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin and logged in users- can get subscribedCatalogue publishedModels'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

        when:
        ListResponse<PublishedModel> publishedModels = (ListResponse<PublishedModel>) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS",
                                                                                          ListResponse)

        then:
        publishedModels
        publishedModels.items.size() == 1

        String jsonData = new File('src/test/resources/subscribedModels.json').text
        ListResponse<PublishedModel> expectedPublishedModels = mapper.readValue(jsonData, ListResponse<PublishedModel>.class)
        publishedModels.items.size() == expectedPublishedModels.items.size()
        publishedModels.items.first() == expectedPublishedModels.items.first()

        logout()
        loginUser()

        when:
        publishedModels = (ListResponse<PublishedModel>) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS",
                                                             ListResponse)
        then:
        then:
        publishedModels
        publishedModels.items.size() == 1

        publishedModels.items.size() == expectedPublishedModels.items.size()
        publishedModels.items.first() == expectedPublishedModels.items.first()

    }

    void 'not logged in - subscribedCatalogue publishedModels endpoint -is unauthorized'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)
        logout()

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS", ListResponse)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }


    void 'admin and logged in users -r - can return subscribedCatalogueTypes'() {
        given:
        loginAdmin()

        when:
        ListResponse<SubscribedCatalogueType> catalogueTypeListResponse =
            (ListResponse<SubscribedCatalogueType>) GET("$SUBSCRIBED_CATALOGUES_PATH$TYPES", ListResponse<SubscribedCatalogueType>)

        then:
        catalogueTypeListResponse
        catalogueTypeListResponse.items.size() == SubscribedCatalogueType.values().size()

        logout()
        loginUser()
        when:
        catalogueTypeListResponse = (ListResponse<SubscribedCatalogueType>) GET("$SUBSCRIBED_CATALOGUES_PATH$TYPES", ListResponse<SubscribedCatalogueType>)

        then:
        catalogueTypeListResponse
        catalogueTypeListResponse.items.size() == SubscribedCatalogueType.values().size()
    }

    void 'not logged in - getSubscribedCatalogueTypes - should throw UNAUTHORIZED '() {
        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH$TYPES", ListResponse<SubscribedCatalogueType>)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin user - can return subscribedCatalogueAuthenticationTypes'() {
        given:
        loginAdmin()

        when:
        ListResponse<SubscribedCatalogueAuthenticationType> authenticationTypeListResponse =
            (ListResponse<SubscribedCatalogueAuthenticationType>) GET("$SUBSCRIBED_CATALOGUES_PATH$AUTHENTICATION_TYPES", ListResponse<SubscribedCatalogueAuthenticationType>)

        then:
        authenticationTypeListResponse
        authenticationTypeListResponse.items.size() == SubscribedCatalogueAuthenticationType.values().size()
    }

    void 'logged-in user or not logged in - subscribedCatalogueAuthenticationTypes - should be forbidden'() {
        given:
        loginUser()

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH$AUTHENTICATION_TYPES", ListResponse<SubscribedCatalogueAuthenticationType>)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH$AUTHENTICATION_TYPES", ListResponse<SubscribedCatalogueAuthenticationType>)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }


    void 'admin User - can update subscribedCatalogue'() {
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

        when:
        SubscribedCatalogue updated =
            (SubscribedCatalogue) PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id", [label: 'updated label'], SubscribedCatalogue)

        then:
        updated
        updated.label == 'updated label'
    }

    void 'logged in user or not logged in - should not be permitted to update subscribedCatalogue'() {
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)
        logout()
        loginUser()

        when:
        PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id", [label: 'updated label'], SubscribedCatalogue)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

        when:
        PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id", [label: 'updated label'], SubscribedCatalogue)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'logged in and admin user -  admin subscribedCatalogues returns list of items to maxValue'() {
        given:
        loginAdmin()

        (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload('label1'), SubscribedCatalogue)
        (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload('label2'), SubscribedCatalogue)

        when:
        ListResponse<SubscribedCatalogue> resp = (ListResponse<SubscribedCatalogue>) GET(SUBSCRIBED_CATALOGUES_PATH, ListResponse, SubscribedCatalogue)

        then:
        resp
        resp.items.size() == 1
        logout()

        loginUser()
        when:
        resp = (ListResponse<SubscribedCatalogue>) GET(SUBSCRIBED_CATALOGUES_PATH, ListResponse, SubscribedCatalogue)

        then:
        resp
        resp.items.size() == 1
    }
}
