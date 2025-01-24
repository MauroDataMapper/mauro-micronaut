package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedModel
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
    void 'logged in admin, - can create #payload for SubscribedCatalogue'() {
        given:
        loginAdmin()
        and:
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)

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
    void 'logged in user, - can create #payload for SubscribedCatalogue'() {
        given:
        loginAdmin()

        and:
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)
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
        SubscribedCatalogue subscribedCatalogue = (SubscribedCatalogue) POST("$ADMIN_SUBSCRIBED_CATALOGUES_PATH", subscribedCataloguePayload(), SubscribedCatalogue)
        logout()

        when:
        POST("$SUBSCRIBED_CATALOGUES_PATH/$subscribedCatalogue.id$SUBSCRIBED_MODELS_PATH", subscribedModelPayload(folderId), SubscribedModel)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNAUTHORIZED
    }

}
