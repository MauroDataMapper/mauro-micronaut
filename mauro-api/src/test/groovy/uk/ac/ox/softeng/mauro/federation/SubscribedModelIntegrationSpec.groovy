package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Unroll

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down-subscribed-catalogue.sql",], phase = Sql.Phase.AFTER_EACH)
class SubscribedModelIntegrationSpec extends SecuredIntegrationSpec {

    static String EXPORTER_URL = "http://maurosandbox.com/sandbox/api/dataModels/0b97751d-b6bf-476c-a9e6-95d3352e8008/export/uk.ac.ox.softeng.maurodatamapper.datamodel." +
                                 "provider." +
                                 "exporter/DataModelJsonExporterService/3.2"
    static String MAURO_DATA_MODEL_CONTENT_TYPE = 'application/mauro.datamodel+json'

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    void setupSpec() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        logout()
    }

    void cleanup() {
        logout()
    }


    @Unroll
    void 'logged in admin, - can create subscribed model #payload'() {
        given:
        loginAdmin()
        and:
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())

        when:
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, payload)

        then:
        subscribedModel
        subscribedModel.subscribedCatalogue.id == subscribedCatalogue.id
        subscribedModel.localModelId

        ListResponse<DataModel> localDataModels = dataModelApi.listAll()
        localDataModels
        localDataModels.items?.size() == 1
        localDataModels.items.id.first() == subscribedModel.localModelId

        where:
        payload                                                                                        | _
        subscribedModelPayload(folderId)                                                               | _
        subscribedModelAndUrlPayload(folderId, EXPORTER_URL)                                           | _
        subscribedModelUrlAndContentTypePayload(folderId, EXPORTER_URL, MAURO_DATA_MODEL_CONTENT_TYPE) | _
        subscribedModelAndImporterProviderServicePayload(folderId)                                     | _

    }

    @Unroll
    void 'logged in user, - can create subscribed model #payload'() {
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        logout()
        loginUser()

        when:
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, payload)

        then:
        subscribedModel
        subscribedModel.subscribedCatalogue.id == subscribedCatalogue.id
        subscribedModel.localModelId

        logout()
        loginAdmin()

        ListResponse<DataModel> localDataModels = dataModelApi.listAll()
        localDataModels
        localDataModels.items?.size() == 1
        localDataModels.items.id.first() == subscribedModel.localModelId

        where:
        payload                                                                                        | _
        subscribedModelPayload(folderId)                                                               | _
        subscribedModelAndUrlPayload(folderId, EXPORTER_URL)                                           | _
        subscribedModelUrlAndContentTypePayload(folderId, EXPORTER_URL, MAURO_DATA_MODEL_CONTENT_TYPE) | _
        subscribedModelAndImporterProviderServicePayload(folderId)                                     | _
    }

    void 'not logged in, should not be able to create subscribed model'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        logout()

        when:
        subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }


    void 'any user can retrieve subscribed model by id'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        logout()

        when:
        loginUser()
        SubscribedModel getResponse = subscribedModelApi.show(subscribedCatalogue.id, subscribedModel.id)

        then:
        getResponse
        logout()

        loginAdmin()
        when:
        getResponse = subscribedModelApi.show(subscribedCatalogue.id, subscribedModel.id)

        then:
        getResponse
    }

    void 'notLoggedIn -get subscribedModel by id - should get unauthorized exception'(){
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        logout()

        when:
        subscribedModelApi.show(subscribedCatalogue.id, subscribedModel.id)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

    }


    void 'importing subscribedModel - should not allow import when existing model exists with same label and modelVersion -throws Unprocessible Entity exception'(){
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        logout()
        loginUser()

        when:
        subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'any user can retrieve subscribed model by id'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        logout()

        when:
        loginUser()
        SubscribedModel getResponse = subscribedModelApi.show(subscribedCatalogue.id, subscribedModel.id)

        then:
        getResponse
        logout()

        loginAdmin()
        when:
        getResponse = subscribedModelApi.show(subscribedCatalogue.id, subscribedModel.id)

        then:
        getResponse
    }

    void 'only adminUser can delete subscribed model'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload())
        SubscribedModel subscribedModel = subscribedModelApi.create(subscribedCatalogue.id, subscribedModelPayload(folderId))
        logout()
        loginUser()

        when:
        subscribedModelApi.delete(subscribedCatalogue.id, subscribedModel.id, new SubscribedModel())
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()

        when:
        HttpResponse httpResponse = subscribedModelApi.delete(subscribedCatalogue.id, subscribedModel.id, new SubscribedModel())

        then:
        httpResponse.status() == HttpStatus.NO_CONTENT
    }

    void 'Delete subscribedCatalogue. AdminUser can delete subscribedCatalogue and associated subscribedModels'() {
        given:
        loginAdmin()

        SubscribedCatalogue created = subscribedCatalogueApi.create(mauroJsonSubscribedCataloguePayload('label1'))
        UUID folderId = folderApi.create(folder()).id
        SubscribedModel subscribedModel = subscribedModelApi.create(created.id, subscribedModelPayload(folderId))

        when:
        HttpResponse httpResponse = subscribedCatalogueApi.delete(created.id, new SubscribedCatalogue())

        then:
        httpResponse.status() == HttpStatus.NO_CONTENT

        when:
        ListResponse<SubscribedCatalogue> listResponse  = subscribedCatalogueApi.listAll()

        then:
        listResponse.items.isEmpty()


        when:
        ListResponse<SubscribedModel> subscribedModelListResponse = subscribedModelApi.listAll(created.id)

        then:
        subscribedModelListResponse.items.isEmpty()

    }
}
