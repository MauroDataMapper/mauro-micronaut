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
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
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

    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject DataTypeApi dataTypeApi
    @Inject DataClassApi dataClassApi
    @Inject DataElementApi dataElementApi
    @Inject DataFlowApi dataFlowApi
    @Inject DataClassComponentApi dataClassComponentApi
    @Inject DataElementComponentApi dataElementComponentApi


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