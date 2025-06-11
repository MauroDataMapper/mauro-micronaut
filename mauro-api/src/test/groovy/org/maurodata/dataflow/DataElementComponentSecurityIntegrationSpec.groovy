package org.maurodata.dataflow

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.api.dataflow.DataClassComponentApi
import org.maurodata.api.dataflow.DataElementComponentApi
import org.maurodata.api.dataflow.DataFlowApi
import org.maurodata.api.datamodel.DataClassApi
import org.maurodata.api.datamodel.DataElementApi
import org.maurodata.api.datamodel.DataModelApi
import org.maurodata.api.datamodel.DataTypeApi
import org.maurodata.api.folder.FolderApi
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataElementComponentSecurityIntegrationSpec extends SecuredIntegrationSpec {

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
        logout()
    }

    void 'create dataElementComponent as admin user'() {
        given:
        loginAdmin()
        when:
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        then:
        dataElementComponentId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataElementComponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        and:
        logout()
        loginUser()
        when:
            dataElementComponentApi.show(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        dataElementComponentApi.update(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId, new DataElementComponent(label: 'renamed label'))
        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'should throw forbidden exception -when non admin user retrieves  admin created dataclasscomponent data'() {
        given:
        loginAdmin()
        UUID dataElementComponentId =
            dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        and:
        logout()
        loginUser()
        when:
        dataElementComponentApi.list(targetId,dataFlowId,dataClassComponentId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()

        DataElementComponent updated =
                dataElementComponentApi.update(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId, new DataElementComponent(label: 'renamed label'))

        then:
        updated

        when:
        logout()
        loginUser()

        dataElementComponentApi.update(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId, new DataElementComponent(label: 'renamed label'))

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void ' non admin user -should not be able to add dataelement to dataelementcomponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        and:
        logout()
        loginUser()
        when:

        dataElementComponentApi.updateSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)

        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()
        DataElementComponent dataElementComponent = dataElementComponentApi.updateSource(sourceId,dataFlowId,dataClassComponentId,dataElementComponentId,dataElementId)
        then:
        dataElementComponent
    }


    void ' non admin user -should not be able to delete dataElement from dataElementComponent'() {
        given:
        loginAdmin()
        UUID dataElementComponentId =
                dataElementComponentApi.create(sourceId, dataFlowId, dataClassComponentId, new DataElementComponent(label: 'test data element component')).id

        and:
        dataElementComponentApi.updateSource(sourceId, dataFlowId, dataClassComponentId,dataElementComponentId,dataElementId)

        when:
        logout()
        loginUser()
        HttpResponse httpResponse = dataElementComponentApi.deleteSource(sourceId, dataFlowId, dataClassComponentId, dataElementComponentId, dataElementId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}