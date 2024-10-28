package uk.ac.ox.softeng.mauro.dataflow

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataElementComponentSecurityIntegrationSpec extends SecuredIntegrationSpec {

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
    UUID dataElementId

    @Shared
    DataType dataType
    @Shared
    UUID dataClassId
    @Shared
    UUID dataClassComponentId

    void setup() {
        loginAdmin()
        folderId = ((Folder) POST("$FOLDERS_PATH", [label: 'folder label root folder'], Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
        dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id

        dataType = (DataType) POST("$DATAMODELS_PATH/$sourceId/dataTypes", [label: 'integer', description: 'a whole number, may be positive or negative, with no maximum or minimum', domainType: 'PrimitiveType'], DataType)
        dataClassId = ((DataClass) POST("$DATAMODELS_PATH/$sourceId$DATACLASSES_PATH", dataClassPayload(), DataClass)).id
        dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", genericModelPayload('data class component label'), DataClassComponent)).id
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$sourceId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH", [label: 'First data element', description: 'The first data element', dataType: [id: dataType.id]], DataElement)).id
        logout()
    }

    void 'create dataElementComponent as admin user'() {
        given:
        loginAdmin()
        when:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id

        then:
        dataElementComponentId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataElementComponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id

        and:
        logout()
        loginUser()
        when:
        (DataElementComponent) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", DataElementComponent)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        (DataElementComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", genericModelPayload('renamed label'), DataElementComponent)
        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'should throw forbidden exception -when non admin user retrieves  admin created dataclasscomponent data'() {
        given:
        loginAdmin()
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id

        and:
        logout()
        loginUser()
        when:
        (ListResponse<DataElementComponent>) GET("/dataModels/$targetId/dataFlows/$dataFlowId/dataClassComponents/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", ListResponse, DataElementComponent)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()

        DataElementComponent updated = (DataElementComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId",
                [label: 'updated label text'], DataElementComponent)

        then:
        updated

        when:
        logout()
        loginUser()

        (DataElementComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId",
                [label: 'another update  label text'], DataElementComponent)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void ' non admin user -should not be able to add dataelement to dataelementcomponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id

        and:
        logout()
        loginUser()
        when:

        (DataElementComponent) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId",
                DataElementComponent)

        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()
        String dataElementComponentString = PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", String)

        then:
        dataElementComponentString
    }


    void ' non admin user -should not be able to delete dataElement from dataElementComponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id

        and:
        PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId",
                String)

        when:
        logout()
        loginUser()
        DELETE("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", HttpStatus)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}