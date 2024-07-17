package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-reference-file.sql", phase = Sql.Phase.AFTER_EACH)
class ReferenceFileIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId
    @Shared
    UUID dataModelId


    void setup() {
        loginAdmin()
        folderId = ((Folder) POST("$FOLDERS_PATH", [label: 'Folder test'], Folder)).id
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)).id
        logout()
    }

    void 'list empty ReferenceFiles '() {
        given:
        loginAdmin()
        when:
        ListResponse<ReferenceFile> responses =
                (ListResponse<ReferenceFile>) GET("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", ListResponse<ReferenceFile>)

        then:
        responses.items.isEmpty()

        when:
        loginUser()
        (ListResponse<ReferenceFile>) GET("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", ListResponse<ReferenceFile>)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

    }

    void 'post referenceFile -should create'() {
        given:
        loginAdmin()
        when:
        ReferenceFile saved = (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)

        then:
        saved
        saved.domainType == ReferenceFile.simpleName

        when:
        logout()
        loginUser()
        (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'get referenceFile by Id -should return just fileContents  '() {
        given:
        loginAdmin()
        String fileContent ="file contents string the quick brown fox jumped over the green hedge and over the gatepost."
        ReferenceFile saved = (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload("testfile",fileContent), ReferenceFile)
        when:
        byte[] retrieved = (byte[]) GET("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH/$saved.id", byte[])

        then:
        retrieved
        retrieved == fileContent.bytes

        logout()
        when:
        loginUser()
        GET("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH/$saved.id", ReferenceFile)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }


    void 'update referenceFile -by adminUser only'() {
        given:
        loginAdmin()
        ReferenceFile saved = (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)
        String fileName = 'new file name'
        when:
        ReferenceFile updated = (ReferenceFile) PUT("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH/$saved.id", referenceFilePayload(fileName), ReferenceFile)

        then:
        updated
        updated.fileName == fileName

        when:
        logout()
        loginUser()
        (ReferenceFile) PUT("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH/$saved.id", referenceFilePayload(fileName), ReferenceFile)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'delete referenceFile -by adminUser only'() {
        given:
        loginAdmin()
        ReferenceFile saved = (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)

        when:
        HttpStatus status = (HttpStatus) DELETE("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH/$saved.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT


        when:
        ListResponse<ReferenceFile> response = (ListResponse<ReferenceFile>) GET("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", ListResponse<ReferenceFile>)

        then: 'the list endpoint shows the update'
        response
        response.items.isEmpty()

        when:
        logout()
        loginUser()
        (ReferenceFile) POST("$DATAMODELS_PATH/$dataModelId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}