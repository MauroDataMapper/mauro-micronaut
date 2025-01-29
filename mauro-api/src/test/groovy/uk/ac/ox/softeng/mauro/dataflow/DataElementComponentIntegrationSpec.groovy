package uk.ac.ox.softeng.mauro.dataflow

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
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

    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject DataTypeApi dataTypeApi
    @Inject DataClassApi dataClassApi
    @Inject DataElementApi dataElementApi
    @Inject DataFlowApi dataFlowApi
    @Inject DataClassComponentApi dataClassComponentApi
    @Inject DataElementComponentApi dataElementComponentApi

    void setup() {
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id

        dataType = dataTypeApi.create(sourceId, new DataType(
                label: 'integer',
                description: 'a whole number, may be positive or negative, with no maximum or minimum',
                dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))

        dataClassId = dataClassApi.create(sourceId, dataClassPayload()).id
        dataElementId = dataElementApi.create(sourceId, dataClassId, new DataElement(
                label: 'First data element',
                description: 'The first data element',
                dataType: dataType)).id

        dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id
        dataClassComponentId = dataClassComponentApi.create(
                sourceId, dataFlowId, new DataClassComponent(label: 'test data class component label')).id
    }

    void 'should create DataElementComponent'() {
        when:
        DataElementComponent response =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component'))

        then:
        response
        response.id
        response.dataClassComponent.id == dataClassComponentId
    }

    void 'should update DataElementComponent'() {
        given:
        DataElementComponent dataElementComponent =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component'))

        when:
        DataElementComponent updated =
                dataElementComponentApi.update(sourceId, dataFlowId, dataClassComponentId, dataElementComponent.id, new DataElementComponent(label: 'renamed label'))
        then:
        updated
        updated.label != dataElementComponent.label
    }

    void 'should list DataElementComponents'() {
        given:
        DataElementComponent response =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component'))
        when:
        ListResponse<DataElementComponent> listResponse =
                dataElementComponentApi.list(sourceId,dataFlowId,dataClassComponentId)
        then:
        listResponse
        listResponse.items.size() == 1
    }

    void 'should add source dataElement to DataElementComponent'() {
        given:
        UUID dataElementComponentId =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        when:
        DataElementComponent dataElementComponent =
            dataElementComponentApi.updateSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        then:
        dataElementComponent

        when:
        dataElementComponent =
            dataElementComponentApi.show(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId)
        then:
        dataElementComponent
        dataElementComponent.dataClassComponent
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1
        !dataElementComponent.targetDataElements
    }


    void 'should delete dataElement from DataElementComponent'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        DataElementComponent dataElementComponent =
                dataElementComponentApi.show(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId)
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1

        when:
        HttpResponse httpResponse =
                dataElementComponentApi.deleteSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)
        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        DataElementComponent updated = dataElementComponentApi.show(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId)
        then:
        updated
        !updated.sourceDataElements
    }

    void 'delete dataElement from DataElementComponent -dataElement not associated -should throw NOT_FOUND'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        when:
        // no data exists
        HttpResponse httpResponse = dataElementComponentApi.deleteTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)
        then:
        httpResponse.status == HttpStatus.NOT_FOUND
    }

    void 'adding duplicate dataElement DataElementComponent  -should throw BAD_REQUEST'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        when:
        //already exists
        dataElementComponentApi.updateTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

    void 'delete DataElementComponent -should delete associated source and target dataElements'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)
        dataElementComponentApi.updateTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        when:
        HttpResponse httpResponse =
                dataElementComponentApi.delete(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId, new DataElementComponent(label: 'test data element component'))

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        DataElementComponent dataElementComponent = dataElementComponentApi.show(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId)
        then:
        !dataElementComponent
    }
}
