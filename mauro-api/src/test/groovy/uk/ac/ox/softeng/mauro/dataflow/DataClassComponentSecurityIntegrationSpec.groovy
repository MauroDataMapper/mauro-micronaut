package uk.ac.ox.softeng.mauro.dataflow

import uk.ac.ox.softeng.mauro.api.dataflow.DataClassComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataFlowApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassComponentSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId
    @Shared
    UUID dataFlowId

    @Shared
    UUID dataClassId

    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject DataClassApi dataClassApi
    @Inject DataFlowApi dataFlowApi
    @Inject DataClassComponentApi dataClassComponentApi


    void setup() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id

        dataClassId = dataModelApi.create(folderId, dataModelPayload('test data class label')).id
        logout()
    }

    void 'create dataClassComponent as admin user'() {
        when:
        loginAdmin()
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id

        then:
        dataClassComponentId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataClassComponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id


        and:
        logout()
        loginUser()
        when:
        DataClassComponent dataClassComponent =
            dataClassComponentApi.show(targetId, dataFlowId, dataClassComponentId)

        then:
        !dataClassComponent
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:

        DataClassComponent updated =
            dataClassComponentApi.update(targetId, dataFlowId, dataClassComponentId,
                                         new DataClassComponent(label: 'renamed label'))
        then:
        !updated
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'should throw forbidden exception -when non admin user retrieves  admin created dataclasscomponent data'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
            ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("label text"), DataClassComponent)).id

        and:
        logout()
        loginUser()
        when:
        ListResponse<DataClassComponent> listResponse = (ListResponse<DataClassComponent>) GET("/dataModels/$targetId/dataFlows/$dataFlowId/dataClassComponents", ListResponse, DataClassComponent)

        then:
        !listResponse
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()

        DataClassComponent updated = (DataClassComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId",
                [label: 'updated label text'], DataClassComponent)

        then:
        updated

        when:
        logout()
        loginUser()

         (DataClassComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId",
                [label: 'another update  label text'], DataClassComponent)
        then:

        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void ' non admin user -should not be able to add dataclass to dataclasscomponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("label text"), DataClassComponent)).id

        and:
        logout()
        loginUser()
        when:
        (DataClassComponent) PUT("/dataModels/$targetId/dataFlows/$dataFlowId/dataClassComponents/$dataClassComponentId/target/$dataClassId", DataClassComponent)

        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()
        String dataClassComponentString = PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassId", String)
        then:
        dataClassComponentString
    }


    void ' non admin user -should not be able to delete dataclass from dataclasscomponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("label text"), DataClassComponent)).id

        and:
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassId", String)

        when:
        logout()
        loginUser()
        DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassId", HttpStatus)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}