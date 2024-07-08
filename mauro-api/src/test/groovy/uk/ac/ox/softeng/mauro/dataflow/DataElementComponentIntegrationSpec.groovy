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
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataElementComponentIntegrationSpec extends CommonDataSpec {

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
    DataType dataType

    @Shared
    UUID dataClassId
    @Shared
    UUID dataClassComponentId
    @Shared
    UUID dataElementId

    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
        
        dataType = (DataType) POST("$DATAMODELS_PATH/$sourceId/dataTypes", [label: 'integer', description: 'a whole number, may be positive or negative, with no maximum or minimum', domainType: 'PrimitiveType'], DataType)
        dataClassId = ((DataClass) POST("$DATAMODELS_PATH/$sourceId$DATACLASSES_PATH", dataClassPayload(), DataClass)).id

        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$sourceId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH", [label: 'First data element', description: 'The first data element', dataType: [id: dataType.id]], DataElement)).id

        dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id
        dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
    }

    void 'should create DataElementComponent'() {
        when:
        DataElementComponent response = (DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload(), DataElementComponent)

        then:
        response
        response.id
        response.dataClassComponent.id == dataClassComponentId
    }

    void 'should update DataElementComponent'() {
        given:
        DataElementComponent dataElementComponent = (DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)

        when:
        DataElementComponent updated = (DataElementComponent) PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponent.id", dataModelPayload('renamed label'), DataElementComponent)
        then:
        updated
        updated.label != dataElementComponent.label
    }

    void 'should list DataElementComponents'() {
        given:
        (DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)
        when:
        ListResponse<DataElementComponent> listResponse = (ListResponse<DataElementComponent>) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", ListResponse<DataElementComponent>)
        then:
        listResponse
        listResponse.items.size() == 1
    }

    void 'should add source dataElement to DataElementComponent'() {
        given:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH",  dataModelPayload('test data class component label'), DataElementComponent)).id
        when:
        String dataElementComponentString = PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", String)

        then:
        dataElementComponentString

        when:
        DataElementComponent dataElementComponent = (DataElementComponent) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", DataElementComponent)
        then:
        dataElementComponent
        dataElementComponent.dataClassComponent
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1
        !dataElementComponent.targetDataElements
    }


    void 'should delete dataElement from DataElementComponent'() {
        given:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", String)

        DataElementComponent dataElementComponent = (DataElementComponent) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", DataElementComponent)
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1

        when:
        HttpStatus httpStatus = DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", HttpStatus)
        then:
        httpStatus == HttpStatus.NO_CONTENT

        when:
        DataElementComponent updated = (DataElementComponent) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", DataElementComponent)
        then:
        updated
        !updated.sourceDataElements
    }

    void 'delete dataElement from DataElementComponent -dataElement not associated -should throw NOT_FOUND'() {
        given:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id
        when:
        //no data exists
        DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$TARGET/$dataElementId", HttpStatus)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'adding duplicate dataElement DataElementComponent  -should throw BAD_REQUEST'() {
        given:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$TARGET/$dataElementId", String)

        when:
        //already exists
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$TARGET/$dataElementId", String)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

    void 'delete DataElementComponent -should delete associated source and target dataElements'() {
        given:
        UUID dataElementComponentId = ((DataElementComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataElementComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", String)
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$TARGET/$dataElementId", String)

        when:
        HttpStatus status = DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$SOURCE/$dataElementId", HttpStatus)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$DATA_ELEMENT_COMPONENTS_PATH/$dataElementComponentId$TARGET/$dataElementId", HttpStatus)
        then:
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }
}
