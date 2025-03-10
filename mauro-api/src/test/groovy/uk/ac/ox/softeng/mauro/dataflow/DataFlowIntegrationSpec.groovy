package uk.ac.ox.softeng.mauro.dataflow

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.api.dataflow.DataClassComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataElementComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataFlowApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataFlowIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId

    @Shared
    UUID dataClassId
    @Shared
    UUID dataElementId

    void setup() {
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
    }

    void 'create dataflow -should create with different source and target'() {
        when:
        DataFlow response =
                dataFlowApi.create(targetId, dataFlowPayload(sourceId))

        then:
        response
        response.id
        response.source.id == sourceId
        response.target.id == targetId
    }

    void 'create dataflow -should create with same source and target'() {
        when:
        DataFlow response =
                dataFlowApi.create(targetId, dataFlowPayload(targetId))

        then:
        response
        response.id
        response.source.id == targetId
        response.target.id == targetId
        response.source.description == 'test description'
    }

    void 'create dataflow -should return http status NotFound when source not found'() {
        when:
        DataFlow response =
                dataFlowApi.create(targetId, dataFlowPayload(UUID.randomUUID()))

        then:
        !response
        //HttpClientResponseException exception = thrown()
        //exception.status == HttpStatus.NOT_FOUND
    }

    void 'create dataflow -should return internal server error when target is invalid id'() {
        when:
        DataFlow response =
                dataFlowApi.create(UUID.randomUUID(), dataFlowPayload(sourceId))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.INTERNAL_SERVER_ERROR
    }


    void 'update dataflow -should update item'() {
        given:
        DataFlow response =
                dataFlowApi.create(targetId, dataFlowPayload(sourceId))

        response
        !response.diagramLayout

        when:
        DataFlow updated = dataFlowApi.update(targetId, response.id,
                new DataFlow(
                        source: new DataModel(id: sourceId),
                        diagramLayout: 'random diagram layout text'))

        then:
        updated
        updated.diagramLayout
    }

    void 'get dataFlow by dataModel -target and source'() {
        given:
        DataFlow dataFlow =
                dataFlowApi.create(targetId, dataFlowPayload(sourceId))
        dataFlow

        when:
        ListResponse<DataFlow> dataflowList = dataFlowApi.list(targetId, null)

        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id

        when:
        dataflowList = dataFlowApi.list(targetId, Type.TARGET)
        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id

        when:
        dataflowList = dataflowList = dataFlowApi.list(sourceId, Type.SOURCE)
        then:
        dataflowList
        dataflowList.items.size() == 1
        dataflowList.items[0].id == dataFlow.id
    }

    void 'delete dataFlow - should delete dataflow and all associated objects'() {
        given:
        dataClassId = dataClassApi.create(sourceId, dataClassPayload('source label')).id

        UUID dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id

        UUID dataClassComponentId = dataClassComponentApi.create(targetId, dataFlowId, new DataClassComponent(label: 'test data class component label')).id

        dataClassComponentApi.updateSource(targetId,dataFlowId,dataClassComponentId,dataClassId)
        dataClassComponentApi.updateTarget(targetId,dataFlowId,dataClassComponentId,dataClassId)

        DataType dataType = dataTypeApi.create(sourceId, new DataType(
                label: 'integer',
                description: 'a whole number, may be positive or negative, with no maximum or minimum',
                dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))

        dataElementId = dataElementApi.create(sourceId, dataClassId, new DataElement(
                label: 'First data element',
                description: 'The first data element',
                dataType: dataType)).id

        UUID dataElementComponentId = dataElementComponentApi.create(targetId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data class component label')).id
        dataElementComponentApi.updateSource(targetId, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId)
        dataElementComponentApi.updateTarget(targetId, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId)

        when:
        HttpResponse response = dataFlowApi.delete(targetId, dataFlowId, new DataFlow())

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        DataFlow dataFlow = dataFlowApi.show(targetId, dataFlowId)

        then:
        !dataFlow

    }

}
