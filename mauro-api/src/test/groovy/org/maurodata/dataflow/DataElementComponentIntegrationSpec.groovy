package org.maurodata.dataflow


import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
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
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1
        !dataElementComponent.targetDataElements
    }


    void 'should delete dataElement from DataElementComponent'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateSource(sourceId, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId)

        DataElementComponent dataElementComponent =
            dataElementComponentApi.show(sourceId, dataFlowId, dataClassComponentId, dataElementComponentId)
        dataElementComponent.sourceDataElements
        dataElementComponent.sourceDataElements.size() == 1

        when:
        HttpResponse httpResponse =
            dataElementComponentApi.deleteSource(sourceId, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId)
        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        ListResponse<DataElementComponent> dataElementComponentListResponse = dataElementComponentApi.list(sourceId, dataFlowId, dataClassComponentId)
        then:
        dataElementComponentListResponse.items.size() == 1
        dataElementComponentListResponse.items.first().sourceDataElements.isEmpty()
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

    void 'adding duplicate dataElement DataElementComponent  -should throw UNPROCESSABLE_ENTITY'() {
        given:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        dataElementComponentApi.updateTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        when:
        //already exists
        dataElementComponentApi.updateTarget(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
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
