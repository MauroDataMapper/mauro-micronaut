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
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-dataflow.sql",
        "classpath:sql/tear-down-datamodel.sql",
        "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataFlowSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID sourceId

    @Shared
    UUID targetId


    void setup() {
        loginAdmin()
        folderId = ((Folder) POST("$FOLDERS_PATH", [label: 'folder label root folder'], Folder)).id
        sourceId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('source label'), DataModel)).id
        targetId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload('target label'), DataModel)).id
        logout()
    }

    void 'create dataflow as admin user'() {
        when:
        loginAdmin()
        UUID dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id

        then:
        dataFlowId
    }

    void 'should throw forbidden exception -when non admin user updates or accesses admin user created dataflow'() {
        given:
        loginAdmin()
        UUID dataFlowId = ((DataFlow) POST("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH", dataFlowPayload(sourceId.toString()), DataFlow)).id

        and:
        logout()
        loginUser()
        when:
        DataFlow dataFlow = (DataFlow) GET("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId", DataFlow)

        then:
        !dataFlow
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        DataFlow updated = (DataFlow) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId",
                [source: [id: sourceId], diagramLayout: 'random diagram layout text'], DataFlow)
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
        updated = (DataFlow) PUT("$DATAMODELS_PATH/$targetId$DATA_FLOWS_PATH/$dataFlowId",
                [source: [id: sourceId], diagramLayout: 'random diagram layout text'], DataFlow)

        then:
        updated
    }
}