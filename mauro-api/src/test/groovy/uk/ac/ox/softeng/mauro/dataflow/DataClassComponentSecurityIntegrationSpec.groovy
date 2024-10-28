package uk.ac.ox.softeng.mauro.dataflow

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

    @Inject
    EmbeddedApplication<?> application

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

    void setup() {
        loginAdmin()
        folderId = ((Folder) POST("$FOLDERS_PATH", [label: 'folder label root folder'], Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
        dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id
        dataClassId = ((DataClass) POST("$DATAMODELS_PATH/$targetId$DATACLASSES_PATH", dataClassPayload('test data class label'), DataClass)).id
        logout()
    }

    void 'create dataClassComponent as admin user'() {
        when:
        loginAdmin()
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("data class component label"), DataClassComponent)).id

        then:
        dataClassComponentId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataClassComponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("data class component label"), DataClassComponent)).id

        and:
        logout()
        loginUser()
        when:
        DataClassComponent dataClassComponent = (DataClassComponent) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", DataClassComponent)

        then:
        !dataClassComponent
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:

        DataClassComponent updated = (DataClassComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", genericModelPayload('renamed label'), DataClassComponent)
        then:
        !updated
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'should throw forbidden exception -when non admin user retrieves  admin created dataclasscomponent data'() {
        given:
        loginAdmin()
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload("label text"), DataClassComponent)).id

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