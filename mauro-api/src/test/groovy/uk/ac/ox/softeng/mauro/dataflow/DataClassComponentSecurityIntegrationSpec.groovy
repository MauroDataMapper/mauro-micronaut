package uk.ac.ox.softeng.mauro.dataflow

import uk.ac.ox.softeng.mauro.api.dataflow.DataClassComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataFlowApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassComponentSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId
    @Shared
    UUID dataFlowId

    @Shared
    UUID dataClassId

    void setup() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id

        dataClassId = dataClassApi.create(sourceId, dataClassPayload()).id
        logout()

    }

    void 'create dataClassComponent as admin user'() {
        when:
        loginAdmin()
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id

        then:
        dataClassComponentId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataClassComponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
            dataClassComponentApi.create(sourceId, dataFlowId,
                                         new DataClassComponent(
                                             label: 'test data class component label',
                                             description: 'test description')).id


        and:
        logout()
        loginUser()
        when:
        DataClassComponent dataClassComponent =
            dataClassComponentApi.show(targetId, dataFlowId, dataClassComponentId)

        then:
        !dataClassComponent
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:

        DataClassComponent updated =
            dataClassComponentApi.update(targetId, dataFlowId, dataClassComponentId,
                                         new DataClassComponent(label: 'renamed label'))
        then:
        !updated
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'should throw forbidden exception -when non admin user retrieves  admin created dataclasscomponent data'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
                dataClassComponentApi.create(sourceId, dataFlowId,
                        new DataClassComponent(
                                label: 'test data class component label',
                                description: 'test description')).id

        and:
        logout()
        loginUser()
        when:
        ListResponse<DataClassComponent> listResponse = dataClassComponentApi.list(targetId, dataFlowId)

        then:
        !listResponse
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()

        DataClassComponent updated =
                dataClassComponentApi.update(targetId, dataFlowId, dataClassComponentId, new DataClassComponent(label: 'updated label text'))

        then:
        updated

        when:
        logout()
        loginUser()

        dataClassComponentApi.update(targetId, dataFlowId, dataClassComponentId, new DataClassComponent(label: 'another update  label text'))
        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void ' non admin user -should not be able to add dataclass to dataclasscomponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
                dataClassComponentApi.create(sourceId, dataFlowId,
                        new DataClassComponent(
                                label: 'test data class component label',
                                description: 'test description')).id

        and:
        logout()
        loginUser()
        when:
        DataClassComponent dataClassComponent = dataClassComponentApi.updateTarget(targetId, dataFlowId, dataClassComponentId, dataClassId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
        !dataClassComponent

        when:
        logout()
        loginAdmin()
        dataClassComponent = dataClassComponentApi.updateTarget(targetId, dataFlowId, dataClassComponentId, dataClassId)

        then:
        dataClassComponent
    }


    void ' non admin user -should not be able to delete dataclass from dataclasscomponent'() {
        given:
        loginAdmin()
        UUID dataClassComponentId =
                dataClassComponentApi.create(sourceId, dataFlowId,
                        new DataClassComponent(
                                label: 'test data class component label',
                                description: 'test description')).id

        and:
        dataClassComponentApi.updateTarget(sourceId, dataFlowId, dataClassComponentId, dataClassId)

        when:
        logout()
        loginUser()
        dataClassComponentApi.deleteTarget(sourceId, dataFlowId, dataClassComponentId, dataClassId)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}