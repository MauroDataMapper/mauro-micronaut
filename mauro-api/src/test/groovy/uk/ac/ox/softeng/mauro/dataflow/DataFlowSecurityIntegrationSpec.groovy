package uk.ac.ox.softeng.mauro.dataflow

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
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
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

    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject DataFlowApi dataFlowApi

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
                (ListResponse<DataFlow>) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", ListResponse, DataFlow)
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