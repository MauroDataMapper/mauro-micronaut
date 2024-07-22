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
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassComponentIntegrationSpec extends CommonDataSpec {

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
    UUID dataClassSourceId

    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
        dataClassSourceId = ((DataClass) POST("$DATAMODELS_PATH/$sourceId$DATACLASSES_PATH", dataClassPayload('source label'), DataClass)).id
        dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id
    }

    void 'should create DataClassComponent'() {
        when:
        DataClassComponent response = (DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)

        then:
        response
        response.id
        response.dataFlow.id == dataFlowId
    }

    void 'should update DataClassComponent'() {
        given:
        DataClassComponent dataClassComponent = (DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)
        when:

        DataClassComponent updated = (DataClassComponent) PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponent.id", dataModelPayload('renamed label'), DataClassComponent)
        then:
        updated
        updated.label != dataClassComponent.label
    }

    void 'should list DataClassComponents'() {
        given:
        (DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)
        when:
        ListResponse<DataClassComponent> listResponse = (ListResponse<DataClassComponent>) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", ListResponse<DataClassComponent>)
        then:
        listResponse
        listResponse.items.size() == 1
    }

    void 'should add source dataClass to DataClassComponent'() {
        given:
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
        when:
        String dataClassComponentString = PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$SOURCE/$dataClassSourceId", String)

        then:
        dataClassComponentString

        when:
        DataClassComponent dataClassComponent = (DataClassComponent) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", DataClassComponent)
        then:
        dataClassComponent
        dataClassComponent.dataFlow.source
        dataClassComponent.dataFlow.target
        dataClassComponent.dataFlow.source.description
        dataClassComponent.dataFlow.target.description

        dataClassComponent.sourceDataClasses
        dataClassComponent.sourceDataClasses.size() == 1
        dataClassComponent.sourceDataClasses[0].id == dataClassSourceId
    }

    void 'should delete dataClass from DataClassComponent'() {
        given:
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$SOURCE/$dataClassSourceId", String)

        when:
        HttpStatus httpStatus = DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$SOURCE/$dataClassSourceId", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        DataClassComponent dataClassComponent = (DataClassComponent) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", DataClassComponent)
        dataClassComponent
        !dataClassComponent.sourceDataClasses

    }

    void 'delete dataClass from DataClassComponent -dataClass not associated -should throw NOT_FOUND'() {
        given:
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
        when:
        //no data exists
        DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassSourceId", HttpStatus)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'adding duplicate dataClass to DataClassComponent  -should throw BAD_REQUEST'() {
        given:
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassSourceId", String)

        when:
        //already exists
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassSourceId", String)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

    void 'delete dataClassComponent -should delete associated source and target dataClasses'() {
        given:
        UUID dataClassComponentId = ((DataClassComponent) POST("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH", dataModelPayload('test data class component label'), DataClassComponent)).id
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$SOURCE/$dataClassSourceId", String)
        PUT("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId$TARGET/$dataClassSourceId", String)

        when:
        HttpStatus status = DELETE("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH/$dataFlowId$DATA_CLASS_COMPONENTS_PATH/$dataClassComponentId", DataClassComponent)
        then:
        HttpClientResponseException exc = thrown()
    }
}
