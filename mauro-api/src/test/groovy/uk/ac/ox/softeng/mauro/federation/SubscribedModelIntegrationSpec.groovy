package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

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
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
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
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)

        when:
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 payload, SubscribedModel)

        then:
        subscribedModel
        subscribedModel.subscribedCatalogue.id == subscribedCatalogue.id
        subscribedModel.localModelId

        ListResponse<DataModel> localDataModels = (ListResponse<DataModel>) GET("$DATAMODELS_PATH", ListResponse)
        localDataModels
        localDataModels.items?.size() == 1
        localDataModels.items.id.first() == subscribedModel.localModelId.toString()

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
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        logout()
        loginUser()

        when:
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 payload, SubscribedModel)

        then:
        subscribedModel
        subscribedModel.subscribedCatalogue.id == subscribedCatalogue.id
        subscribedModel.localModelId

        logout()
        loginAdmin()

        ListResponse<DataModel> localDataModels = (ListResponse<DataModel>) GET("$DATAMODELS_PATH", ListResponse)
        localDataModels
        localDataModels.items?.size() == 1
        localDataModels.items.id.first() == subscribedModel.localModelId.toString()

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
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        logout()

        when:
        POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH", subscribedModelPayload(folderId), SubscribedModel)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }


    void 'any user can retrieve subscribed model by id'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 subscribedModelPayload(folderId) , SubscribedModel)
        logout()

        when:
        loginUser()
        SubscribedModel getResponse = (SubscribedModel) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id",SubscribedModel)

        then:
        getResponse
        logout()

        loginAdmin()
        when:
        getResponse = (SubscribedModel) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id",SubscribedModel)

        then:
        getResponse
    }

    void 'notLoggedIn -get subscribedModel by id - should get unauthorized exception'(){
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 subscribedModelPayload(folderId) , SubscribedModel)
        logout()

        when:
        GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id",SubscribedModel)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED

    }


    void 'importing subscribedModel - should not allow import when existing model exists with same label and modelVersion -throws Unprocessible Entity exception'(){
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH", subscribedModelPayload(folderId), SubscribedModel)
        logout()
        loginUser()

        when:
        POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH", subscribedModelPayload(folderId), SubscribedModel)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'any user can retrieve subscribed model by id'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 subscribedModelPayload(folderId) , SubscribedModel)
        logout()

        when:
        loginUser()
        SubscribedModel getResponse = (SubscribedModel) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id",SubscribedModel)

        then:
        getResponse
        logout()

        loginAdmin()
        when:
        getResponse = (SubscribedModel) GET("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id",SubscribedModel)

        then:
        getResponse
    }

    void 'only adminUser can delete subscribed model'() {
        given:
        loginAdmin()
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload(), SubscribedCatalogue)
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH",
                                                                 subscribedModelPayload(folderId), SubscribedModel)
        logout()
        loginUser()

        when:
        DELETE("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id", HttpStatus)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()
        loginAdmin()

        when:
        HttpStatus httpStatus = DELETE("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH/$subscribedModel.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT
    }

    void 'Delete subscribedCatalogue. AdminUser can delete subscribedCatalogue and associated subscribedModels'() {
        given:
        loginAdmin()

        SubscribedCatalogue created = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", mauroJsonSubscribedCataloguePayload('label1'), SubscribedCatalogue)
        UUID folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        SubscribedModel subscribedModel = (SubscribedModel) POST("$SUBSCRIBED_CATALOGUES_PATH/$created.id$SUBSCRIBED_MODELS_PATH", subscribedModelPayload(folderId), SubscribedModel)

        when:
        HttpStatus httpStatus = DELETE("$ADMIN_SUBSCRIBED_CATALOGUES_PATH/$created.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        when:
        ListResponse<SubscribedCatalogue> listResponse  = (ListResponse) GET("$SUBSCRIBED_CATALOGUES_PATH", ListResponse)

        then:
        listResponse.items.isEmpty()


        when:
        ListResponse<SubscribedModel> subscribedModelListResponse = (ListResponse)GET("$SUBSCRIBED_CATALOGUES_PATH/$created.id$SUBSCRIBED_MODELS_PATH", ListResponse)

        then:
        subscribedModelListResponse.items.isEmpty()

    }
}
