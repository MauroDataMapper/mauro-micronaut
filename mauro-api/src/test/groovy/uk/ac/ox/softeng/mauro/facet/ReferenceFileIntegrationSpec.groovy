package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@SecuredContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-reference-file.sql", phase = Sql.Phase.AFTER_EACH)
class ReferenceFileIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId
    @Shared
    UUID dataModelId


    void setup() {
        loginAdmin()
        folderId = folderApi.create(new Folder(label: 'Folder test')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload()).id
        logout()
    }

    void 'list empty ReferenceFiles '() {
        given:
        loginAdmin()
        when:
        ListResponse<ReferenceFile> responses = referenceFileApi.list("DataModel", dataModelId)

        then:
        responses.items.isEmpty()

        when:
        loginUser()
        responses = referenceFileApi.list("DataModel", dataModelId)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

    }

    void 'post referenceFile -should create'() {
        given:
        loginAdmin()
        when:
        ReferenceFile saved = referenceFileApi.create("DataModel", dataModelId, referenceFilePayload())

        then:
        saved
        saved.domainType == ReferenceFile.simpleName

        when:
        logout()
        loginUser()
        referenceFileApi.create("DataModel", dataModelId, referenceFilePayload())
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'get referenceFile by Id -should return just fileContents  '() {
        given:
        loginAdmin()
        String fileContent = "file contents string the quick brown fox jumped over the green hedge and over the gatepost."
        ReferenceFile saved = referenceFileApi.create("DataModel", dataModelId, referenceFilePayload("testfile", fileContent))
        when:
        byte[] retrieved = referenceFileApi.showAndReturnFile("DataModel", dataModelId, saved.id)

        then:
        retrieved
        retrieved == fileContent.bytes

        logout()
        when:
        loginUser()
        retrieved = referenceFileApi.showAndReturnFile("DataModel", dataModelId, saved.id)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'update referenceFile -by adminUser only'() {
        given:
        loginAdmin()
        ReferenceFile saved = referenceFileApi.create("DataModel", dataModelId, referenceFilePayload())
        String fileName = 'new file name'
        String updatedFileContents = 'an updated sentence .. it was a cold frostly dry morning when I started to walk to the woods'
        when:
        ReferenceFile updated = referenceFileApi.update("DataModel", dataModelId, saved.id, referenceFilePayload(fileName, updatedFileContents))

        then:
        updated
        updated.fileName == fileName
        updated.fileSize == updatedFileContents.size()
        when:
        byte[] retrieved = referenceFileApi.showAndReturnFile("DataModel", dataModelId, saved.id)
        then:

        retrieved
        retrieved == updatedFileContents.bytes

        when:
        logout()
        loginUser()
        referenceFileApi.update("DataModel", dataModelId, saved.id, referenceFilePayload(fileName))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'delete referenceFile -by adminUser only'() {
        given:
        loginAdmin()
        ReferenceFile saved = referenceFileApi.create("DataModel", dataModelId, referenceFilePayload())

        when:
        HttpResponse response = referenceFileApi.delete("DataModel", dataModelId, saved.id)

        then:
        response.status == HttpStatus.NO_CONTENT


        when:
        ListResponse<ReferenceFile> responseList = referenceFileApi.list("DataModel", dataModelId)

        then: 'the list endpoint shows the update'
        responseList
        responseList.items.isEmpty()

        when:
        logout()
        loginUser()
        referenceFileApi.create("DataModel", dataModelId, referenceFilePayload())
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}