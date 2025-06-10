package org.maurodata.dataflow


import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassComponentIntegrationSpec extends CommonDataSpec {

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

    @Shared
    UUID dataClassTargetId

    void setup() {
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        dataClassSourceId = dataClassApi.create(sourceId, dataClassPayload('source label')).id
        dataClassTargetId = dataClassApi.create(targetId, dataClassPayload('target label')).id
        dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id
    }

    void 'should create DataClassComponent'() {
        when:
        DataClassComponent response =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description'))

        then:
        response
        response.id
        response.dataFlow.id == dataFlowId
    }

    void 'should update DataClassComponent'() {
        given:
        DataClassComponent dataClassComponent =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description'))
        when:

        DataClassComponent updated = dataClassComponentApi.update(sourceId, dataFlowId, dataClassComponent.id,
                                                                  new DataClassComponent(label: 'renamed label'))
        then:
        updated
        updated.label != dataClassComponent.label
    }

    void 'should list DataClassComponents'() {
        given:
        dataClassComponentApi.create(sourceId, dataFlowId,
                                     new DataClassComponent(
                                         label: 'test data class component label',
                                         description: 'test description'))
        when:
        ListResponse<DataClassComponent> listResponse = dataClassComponentApi.list(sourceId,dataFlowId)
        then:
        listResponse
        listResponse.items.size() == 1
    }

    void 'should add source dataClass to DataClassComponent'() {
        given:
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                 new DataClassComponent(
                 label: 'test data class component label',
                 description: 'test description')).id
        when:
        DataClassComponent dcc = dataClassComponentApi.updateSource(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)

        then:
        dcc

        when:
        DataClassComponent dataClassComponent = dataClassComponentApi.show(sourceId, dataFlowId, dataClassComponentId)
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
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                new DataClassComponent(
                label: 'test data class component label',
                description: 'test description')).id

        dataClassComponentApi.updateSource(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)

        when:
        HttpResponse httpResponse = dataClassComponentApi.deleteSource(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        DataClassComponent dataClassComponent = dataClassComponentApi.show(sourceId, dataFlowId, dataClassComponentId)
        dataClassComponent
        !dataClassComponent.sourceDataClasses

    }

    void 'delete dataClass from DataClassComponent -dataClass not associated -should throw NOT_FOUND'() {
        given:
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id
        when:
        //no data exists
        HttpResponse httpResponse = dataClassComponentApi.deleteTarget(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)
        then:
        httpResponse.status == HttpStatus.NOT_FOUND
    }

    void 'adding duplicate dataClass to DataClassComponent  -should throw UNPROCESSABLE_ENTITY'() {
        given:
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id

        dataClassComponentApi.updateTarget(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)

        when:
        //already exists
        dataClassComponentApi.updateTarget(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'delete dataClassComponent -should delete associated source and target dataClasses'() {
        given:
        DataClassComponent dataClassComponent =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description'))

        UUID dataClassComponentId = dataClassComponent.id


        dataClassComponentApi.updateSource(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)
        dataClassComponentApi.updateTarget(sourceId, dataFlowId, dataClassComponentId, dataClassSourceId)

        when:
        HttpResponse httpResponse = dataClassComponentApi.delete(sourceId, dataFlowId, dataClassComponentId, dataClassComponent)

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        def response = dataClassComponentApi.show(sourceId, dataFlowId, dataClassComponentId)
        then:
        !response
        //HttpClientResponseException exception = thrown()
        //exception.status == HttpStatus.NOT_FOUND
    }
}
