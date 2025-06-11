package org.maurodata.federation

import org.maurodata.controller.federation.converter.AtomSubscribedCatalogueConverter
import org.maurodata.domain.facet.federation.PublishedModel
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedCatalogueAuthenticationType
import org.maurodata.domain.facet.federation.SubscribedCatalogueType
import org.maurodata.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.xml.XmlSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Unroll

@Singleton
@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-subscribed-catalogue.sql"], phase = Sql.Phase.AFTER_ALL)
class SubscribedCatalogueIntegrationSpec extends SecuredIntegrationSpec {
    static final String TEST_MODEL_ID = "0b97751d-b6bf-476c-a9e6-95d3352e8008"
    static final String ATOM_MODEL_ID_NEWER_VERSIONS = "urn:uuid:b4484456-366a-4430-a8ae-56248003fc5a"

    @Inject
    ObjectMapper mapper
    @Shared
    UUID subscribedCatalogueId

    void setupSpec(){
        loginAdmin()
        subscribedCatalogueId =  subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload()).id
        logout()
    }

    void cleanup() {
        logout()
    }

    void 'logged in and admin user - can access admin subscribedCatalogues'() {
        given:
        loginAdmin()

        when:
        ListResponse<SubscribedCatalogue> getAllResp = subscribedCatalogueApi.listAll()

        then:
        getAllResp
        getAllResp.count == 1
        logout()

        loginUser()
        when:
        getAllResp = subscribedCatalogueApi.listAll()

        then:
        getAllResp
        getAllResp.count == 1
    }

    void 'user not signed in - cannot access admin subscribedCatalogues endpoint'() {
        when:
        subscribedCatalogueApi.listAll()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'adminUser can create SubscribedCatalogue'() {
        given:
        loginAdmin()

        when:
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(atomSubscribedCataloguePayload())

        then:
        subscribedCatalogue
        subscribedCatalogue.url == "http://localhost:8088/test/syndication.xml"
        subscribedCatalogue.subscribedCatalogueAuthenticationType == SubscribedCatalogueAuthenticationType.API_KEY
        subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.ATOM
        subscribedCatalogue.apiKey == 'b39d63d4-4fd4-494d-a491-3c778d89acae'
    }

    @Unroll
    void '#currentUser cannot create SubscribedCatalogue -result is #exceptionStatus'() {
        given:
        currentUser

        when:
        subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())

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
        SubscribedCatalogue retrieved = subscribedCatalogueApi.show(subscribedCatalogueId)

        then:
        retrieved

        logout()
        loginUser()

        when:
        retrieved = subscribedCatalogueApi.show(subscribedCatalogueId)
        then:
        retrieved
    }

    @Unroll
    void 'not logged in - subscribedCatalogue publishedModels endpoint -is unauthorized'() {

        when:
        subscribedCatalogueApi.publishedModels(subscribedCatalogueId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin user - subscribedCatalogue with #payload -should return successful testConnection endpoint'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(payload)

        when:
        HttpResponse httpResponse = subscribedCatalogueApi.testConnection(subscribedCatalogue.id)

        then:
        httpResponse.status() == HttpStatus.OK

        where:
        payload                               | _
        mauroJsonSubscribedCataloguePayload() | _
        atomSubscribedCataloguePayload()      | _
    }


    void 'non adminUsers test subscribedCatalogue connection - should return exception'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        logout()
        loginUser()

        when:
        subscribedCatalogueApi.testConnection(subscribedCatalogue.id)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        when:
        subscribedCatalogueApi.testConnection(subscribedCatalogue.id)

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    @Unroll
    void 'admin and logged in users- can get #payload type subscribedCatalogue with #expectedPublishedModels and #expectedNumberOfItems'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(payload)

        when:
        ListResponse<PublishedModel> publishedModels = subscribedCatalogueApi.publishedModels(subscribedCatalogue.id)
        then:
        publishedModels

        publishedModels.items.size() == expectedNumberOfItems
        publishedModels.items.size() == expectedPublishedModels.size()
        publishedModels.items.first().modelId == expectedPublishedModels.first().modelId

        logout()
        loginUser()

        when:
        publishedModels = subscribedCatalogueApi.publishedModels(subscribedCatalogue.id)
        then:
        publishedModels


        publishedModels.items.size() == expectedPublishedModels.size()
        publishedModels.items.sort {model -> model.modelId}.first().modelId == expectedPublishedModels.sort {model -> model.modelId}.first().modelId
        publishedModels.items.sort {model -> model.modelId}.first().title == expectedPublishedModels.sort {model -> model.modelId}.first().title
        publishedModels.items.sort {model -> model.modelId}.first().lastUpdated == expectedPublishedModels.sort {model -> model.modelId}.first().lastUpdated
        publishedModels.items.sort {model -> model.modelId}.first().datePublished != null
        publishedModels.items.sort {model -> model.modelId}.first().author == expectedPublishedModels.sort {model -> model.modelId}.first().author
        publishedModels.items.sort {model -> model.modelId}.first().description == expectedPublishedModels.sort {model -> model.modelId}.first().description

        where:
        payload                               | expectedPublishedModels     | expectedNumberOfItems
        mauroJsonSubscribedCataloguePayload() | mauroJsonExpectedResponse() | 1
        atomSubscribedCataloguePayload()      | atomExpectedResponse()      | 1
    }


    @Unroll
    void 'admin and logged in users- should return newerVersions output for subscribedCatalogue #payload with #publishedModelId and #numberOfNewerVersions'() {
        given:
        loginAdmin()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(payload)

        logout()
        loginUser()

        when:
        SubscribedCataloguesPublishedModelsNewerVersions response = subscribedCatalogueApi.publishedModelsNewerVersions(subscribedCatalogue.id, publishedModelId)
        then:
        response
        response.newerPublishedModels.size() == numberOfNewerVersions
        response.lastUpdated.toString() >= lastUpdatedString

        where:
        payload                               | publishedModelId             | numberOfNewerVersions | lastUpdatedString
        mauroJsonSubscribedCataloguePayload() | TEST_MODEL_ID                | 3                     | "2023-06-12T16:03:07.118Z"
        atomSubscribedCataloguePayload()      | ATOM_MODEL_ID_NEWER_VERSIONS | 0                     | "2025-02-17T04:02:14Z"

    }


    void 'admin and logged in users - can return subscribedCatalogueTypes'() {
        given:
        loginAdmin()

        when:
        ListResponse<SubscribedCatalogueType> catalogueTypeListResponse = subscribedCatalogueApi.types()

        then:
        catalogueTypeListResponse
        catalogueTypeListResponse.items.size() == SubscribedCatalogueType.values().size()

        logout()
        loginUser()
        when:
        catalogueTypeListResponse = subscribedCatalogueApi.types()

        then:
        catalogueTypeListResponse
        catalogueTypeListResponse.items.size() == SubscribedCatalogueType.values().size()
    }

    void 'not logged in - getSubscribedCatalogueTypes - should throw UNAUTHORIZED '() {
        when:
        subscribedCatalogueApi.types()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin user - can return subscribedCatalogueAuthenticationTypes'() {
        given:
        loginAdmin()

        when:
        ListResponse<SubscribedCatalogueAuthenticationType> authenticationTypeListResponse = subscribedCatalogueApi.authenticationTypes()

        then:
        authenticationTypeListResponse
        authenticationTypeListResponse.items.size() == SubscribedCatalogueAuthenticationType.values().size()
    }

    void 'logged-in user or not logged in - subscribedCatalogueAuthenticationTypes - should be forbidden'() {
        given:
        loginUser()

        when:
        subscribedCatalogueApi.authenticationTypes()

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

        when:
        subscribedCatalogueApi.authenticationTypes()

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'admin User - can update subscribedCatalogue'() {
        given:
        loginAdmin()

        when:
        SubscribedCatalogue updated = subscribedCatalogueApi.update(subscribedCatalogueId, new SubscribedCatalogue(label: 'updated label'))

        then:
        updated
        updated.label == 'updated label'
    }

    void 'logged in user or not logged in - should not be permitted to update subscribedCatalogue'() {
        given:
        loginUser()

        when:
        subscribedCatalogueApi.update(subscribedCatalogueId, new SubscribedCatalogue(label: 'updated label'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

        when:
        subscribedCatalogueApi.update(subscribedCatalogueId, new SubscribedCatalogue(label: 'updated label'))

        then:
        exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

    void 'logged in and admin user -  admin subscribedCatalogues returns list of items to maxValue'() {
        given:
        loginAdmin()

        subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload('label1'))

        when:
        ListResponse<SubscribedCatalogue> resp = subscribedCatalogueApi.listSubscribedCatalogues()

        then:
        resp
        resp.items.size() == 1
        logout()

        loginUser()
        when:
        resp = (ListResponse<SubscribedCatalogue>) subscribedCatalogueApi.listSubscribedCatalogues()

        then:
        resp
        resp.items.size() == 1
    }

    void 'only adminUser can delete subscribed catalogue'() {
        given:
        loginAdmin()

        SubscribedCatalogue created = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload('label1'))

        when:
        HttpResponse httpResponse = subscribedCatalogueApi.delete(created.id, new SubscribedCatalogue())

        then:
        httpResponse.status() == HttpStatus.NO_CONTENT

        logout()
        loginUser()

        when:
        subscribedCatalogueApi.delete(created.id, new SubscribedCatalogue())

        then:
        HttpClientResponseException httpClientResponseException = thrown()
        httpClientResponseException.status == HttpStatus.FORBIDDEN

    }


    List<PublishedModel> atomExpectedResponse() {
        def atomResults = new XmlSlurper().parse(new File('src/test/resources/xml-federated-data-test.xml'))
        atomResults.entry.collect { AtomSubscribedCatalogueConverter.convertEntryToPublishedModel(atomResults, it)}.sort {l, r ->
            r.lastUpdated <=> l.lastUpdated ?:
            l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
            l.modelLabel <=> r.modelLabel ?:
            l.modelId <=> r.modelId
        }
    }

    List<PublishedModel> mauroJsonExpectedResponse() {
        //List<Object> list = mapper.readValue(new File('src/test/resources/mauroJsonPublishedModels.json').text, List<PublishedModel>)
        //List<PublishedModel> result = list.collect {(PublishedModel) mapper.readValue(it, PublishedModel)}

        List<PublishedModel> result = mapper.readValue(new File('src/test/resources/mauroJsonPublishedModels.json').text, new TypeReference<List<PublishedModel>>(){})
        result
    }
}
