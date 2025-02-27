package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.controller.federation.converter.AtomSubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.xml.XmlSlurper
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Unroll

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-subscribed-catalogue.sql"], phase = Sql.Phase.AFTER_ALL)
class SubscribedCatalogueIntegrationSpec extends SecuredIntegrationSpec {
    @Inject
    EmbeddedApplication<?> application
    @Inject
    ObjectMapper mapper
    @Shared
    UUID subscribedCatalogueId

    void setupSpec(){
        loginAdmin()
        subscribedCatalogueId =  ((SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)).id
        logout()
    }

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
        getAllResp.count == 1
        logout()

        loginUser()
        when:
        getAllResp = GET(ADMIN_SUBSCRIBED_CATALOGUES_PATH)

        then:
        getAllResp
        getAllResp.count == 1
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
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", atomSubscribedCataloguePayload(), SubscribedCatalogue)

        then:
        subscribedCatalogue
        subscribedCatalogue.url == "https://ontology.nhs.uk/production1/synd/syndication.xml"
        subscribedCatalogue.subscribedCatalogueAuthenticationType == SubscribedCatalogueAuthenticationType.API_KEY
        subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.ATOM
        subscribedCatalogue.apiKey == 'b39d63d4-4fd4-494d-a491-3c778d89acae'
    }

    @Unroll
    void '#currentUser cannot create SubscribedCatalogue -result is #exceptionStatus'() {
        given:
        currentUser

        when:
        POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == exceptionStatus

        where:
        currentUser | exceptionStatus
        loginUser() | HttpStatus.FORBIDDEN
        null        | HttpStatus.UNAUTHORIZED
    }

    void 'any user - can retrieve subscribedCatalogue by id'() {
        loginAdmin()
        when:
        SubscribedCatalogue retrieved = GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId", SubscribedCatalogue)

        then:
        retrieved

        logout()
        loginUser()

        when:
        retrieved = GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId", SubscribedCatalogue)
        then:
        retrieved
    }

    @Unroll
    void 'not logged in - subscribedCatalogue publishedModels endpoint -is unauthorized'() {

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId$PUBLISHEDMODELS", ListResponse)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin user - subscribedCatalogue with #payload -should return successful testConnection endpoint'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", payload, SubscribedCatalogue)

        when:
        HttpStatus httpStatus = GET("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        httpStatus == HttpStatus.OK

        where:
        payload                               | _
        mauroJsonSubscribedCataloguePayload() | _
        atomSubscribedCataloguePayload()      | _
    }


    void 'non adminUsers test subscribedCatalogue connection - should return exception'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        logout()
        loginUser()

        when:
        GET("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        when:
        GET("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$TEST_CONNECTION", HttpStatus)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    @Unroll
    void 'admin and logged in users- can get #payload type subscribedCatalogue with #expectedPublishedModels and #expectedNumberOfItems'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", payload, SubscribedCatalogue)

        when:
        ListResponse<PublishedModel> publishedModels = (ListResponse<PublishedModel>) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS",
                                                                                          ListResponse)
        then:
        publishedModels

        publishedModels.items.size() == expectedNumberOfItems
        publishedModels.items.size() == expectedPublishedModels.size()
        publishedModels.items.first().modelId == expectedPublishedModels.first().modelId

        logout()
        loginUser()

        when:
        publishedModels = (ListResponse<PublishedModel>) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS",
                                                             ListResponse)
        then:
        publishedModels
        publishedModels.items.size() == expectedPublishedModels.size()
        publishedModels.items.sort {model -> model.modelId}.first().modelId == expectedPublishedModels.sort {model -> model.modelId}.first().modelId
        publishedModels.items.sort {model -> model.modelId}.first().title == expectedPublishedModels.sort {model -> model.modelId}.first().title
        publishedModels.items.sort {model -> model.modelId}.first().lastUpdated == expectedPublishedModels.sort {model -> model.modelId}.first().lastUpdated?.toString()
        publishedModels.items.sort {model -> model.modelId}.first().datePublished == expectedPublishedModels.sort {model -> model.modelId}.first().datePublished?.toString()
        publishedModels.items.sort {model -> model.modelId}.first().author == expectedPublishedModels.sort {model -> model.modelId}.first().author
        publishedModels.items.sort {model -> model.modelId}.first().description == expectedPublishedModels.sort {model -> model.modelId}.first().description

        where:
        payload                               | expectedPublishedModels     | expectedNumberOfItems
        mauroJsonSubscribedCataloguePayload() | mauroJsonExpectedResponse() | 1
        atomSubscribedCataloguePayload()      | atomExpectedResponse()      | 112
    }


    @Unroll
    void 'admin and logged in users- should return newerVersions output for subscribedCatalogue #payload with #publishedModelId and #numberOfNewerVersions'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", payload, SubscribedCatalogue)

        logout()
        loginUser()

        when:
        SubscribedCataloguesPublishedModelsNewerVersions response =
            (SubscribedCataloguesPublishedModelsNewerVersions) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$PUBLISHEDMODELS/$publishedModelId/newerVersions",
                                                                   SubscribedCataloguesPublishedModelsNewerVersions)
        then:
        response
        response.newerPublishedModels.size() == numberOfNewerVersions
        response.lastUpdated.toString() == lastUpdatedString

        where:
        payload                               | publishedModelId             | numberOfNewerVersions | lastUpdatedString
        mauroJsonSubscribedCataloguePayload() | TEST_MODEL_ID                | 3                     | "2023-06-12T16:03:07.118Z"
        atomSubscribedCataloguePayload()      | ATOM_MODEL_ID_NEWER_VERSIONS | 4                     | "2025-02-17T04:02:14Z"

    }




    void 'admin and logged in users - can return subscribedCatalogueTypes'() {
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

        when:
        SubscribedCatalogue updated =
            (SubscribedCatalogue) PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId", [label: 'updated label'], SubscribedCatalogue)

        then:
        updated
        updated.label == 'updated label'
    }

    void 'logged in user or not logged in - should not be permitted to update subscribedCatalogue'() {
        given:
        loginUser()

        when:
        PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId", [label: 'updated label'], SubscribedCatalogue)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

        when:
        PUT("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogueId", [label: 'updated label'], SubscribedCatalogue)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'logged in and admin user -  admin subscribedCatalogues returns list of items to maxValue'() {
        given:
        loginAdmin()

        (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload('label1'), SubscribedCatalogue)

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

    void 'only adminUser can delete subscribed catalogue'() {
        given:
        loginAdmin()

        SubscribedCatalogue created = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload('label1'), SubscribedCatalogue)

        when:
        HttpStatus httpStatus = DELETE("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$created.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        logout()
        loginUser()

        when:
        DELETE("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$created.id", HttpStatus)

        then:
        HttpClientResponseException httpClientResponseException = thrown()
        httpClientResponseException.status == HttpStatus.FORBIDDEN

    }


    List<PublishedModel> atomExpectedResponse() {
        def atomResults = new XmlSlurper().parse(new File('src/test/resources/xml-federated-data.xml'))
        atomResults.entry.collect { AtomSubscribedCatalogueConverter.convertEntryToPublishedModel(atomResults, it)}.sort {l, r ->
            r.lastUpdated <=> l.lastUpdated ?:
            l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
            l.modelLabel <=> r.modelLabel ?:
            l.modelId <=> r.modelId
        }
    }

    List<PublishedModel> mauroJsonExpectedResponse() {
        List<PublishedModel> result = mapper.readValue(new File('src/test/resources/mauroJsonPublishedModels.json').text, List<PublishedModel>)
        result
    }
}
