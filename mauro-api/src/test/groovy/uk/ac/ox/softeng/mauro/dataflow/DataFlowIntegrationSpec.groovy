package uk.ac.ox.softeng.mauro.dataflow

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
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
class DataFlowIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId

    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
    }

    void 'create dataflow -should create with different source and target'() {
        when:
        DataFlow response = (DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)

        then:
        response
        response.id
        response.source.id == sourceId
        response.target.id == targetId
    }

    void 'create dataflow -should create with same source and target'() {
        when:
        DataFlow response = (DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(targetId.toString()), DataFlow)

        then:
        response
        response.id
        response.source.id == targetId
        response.target.id == targetId
    }

    void 'create dataflow -should return http status NotFound when source not found'() {
        when:
        (DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(UUID.randomUUID().toString()), DataFlow)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'create dataflow -should return http status NotFound when target not found'() {
        when:
        (DataFlow) POST("$DATAMODELS_PATH/${UUID.randomUUID()}$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }


    void 'update dataflow -should update item'() {
        given:
        DataFlow response = (DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)
        response
        !response.diagramLayout

        when:
        DataFlow updated = (DataFlow) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$response.id",
                [source: [id: sourceId], diagramLayout: 'random diagram layout text'], DataFlow)

        then:
        updated
        updated.diagramLayout
    }

    void 'get dataFlow by dataModel -target and source'() {
        given:
        DataFlow dataFlow = (DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)
        dataFlow

        when:
        ListResponse<DataFlow> dataflowList = (ListResponse<DataFlow>) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", ListResponse<DataFlow>)

        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id.toString()

        when:
        dataflowList = (ListResponse<DataFlow>) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH?type=target", ListResponse<DataFlow>)
        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id.toString()

        when:
        dataflowList = (ListResponse<DataFlow>) GET("$DATAMODELS_PATH/$sourceId$DATA_FLOWS_PATH?type=source", ListResponse<DataFlow>)
        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id.toString()
    }


}
