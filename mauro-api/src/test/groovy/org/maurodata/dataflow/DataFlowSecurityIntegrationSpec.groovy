package org.maurodata.dataflow

import org.maurodata.domain.dataflow.Type

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
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataModel
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
class DataFlowSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId


    void setup() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        sourceId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        logout()
    }

    void 'create dataflow as admin user'() {
        when:
        loginAdmin()
        UUID dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id

        then:
        dataFlowId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataflow'() {
        given:
        loginAdmin()
        UUID dataFlowId = dataFlowApi.create(targetId, dataFlowPayload(sourceId)).id

        and:
        logout()
        loginUser()
        when:
        DataFlow dataFlow = dataFlowApi.show(targetId, dataFlowId)

        then:
        !dataFlow
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DataFlow updated = dataFlowApi.update(targetId, dataFlowId,
                new DataFlow(source: new DataModel(id: sourceId),
                        diagramLayout: 'random diagram layout text'))
        then:
        !updated
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        ListResponse<DataFlow> listResponse =
                dataFlowApi.list(targetId, Type.TARGET)
        then:
        !listResponse
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        logout()
        loginAdmin()
        and:
        updated = dataFlowApi.update(targetId, dataFlowId,
            new DataFlow(source: new DataModel(id: sourceId),
                    diagramLayout: 'random diagram layout text'))

        then:
        updated
    }
}