package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class CodeSetIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    @AutoCleanup
    @Shared
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID codeSetId

    def setup(){
        def folderPayload = folder()
        def folderResponse = POST(FOLDERS_PATH, folderPayload)
        folderId = UUID.fromString(folderResponse.id as String)
    }

    void 'test post'() {
        when:
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())

        then:
        response
        response.label == "Test code set"
        response.path.toString() == 'cs:Test code set$main'
        response.description == "code set description"
        response.author == "A.N. Other"
        response.organisation == "uk.ac.gridpp.ral.org"
    }


    void 'test codeSet getById'() {
        given:
        def codeSetPayload = codeSet()
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSetPayload)
        codeSetId = UUID.fromString(response.id as String)

        when:
        def getResponse = GET("$CODE_SET_PATH/$codeSetId")

        then:
        getResponse
        getResponse.label == codeSetPayload.label
        getResponse.description == codeSetPayload.description
        getResponse.organisation == codeSetPayload.organisation
        getResponse.author == codeSetPayload.author
    }

    void 'test codeSet listAll'() {
        given:
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())
        codeSetId = UUID.fromString(response.id as String)

        and:
        def folderResp2 = POST(FOLDERS_PATH, [label: 'Test-folder-2'])
        def folderId2 = UUID.fromString(folderResp2.id as String)
        and:
        def codeSetResp2 = POST("$FOLDERS_PATH/$folderId2$CODE_SET_PATH", codeSet())
        def codeSet2Id = UUID.fromString(codeSetResp2.id as String)

        when:
        def getAllResp = GET(CODE_SET_PATH)

        then:
        getAllResp != null
        def actualIds = getAllResp.items.id.collect()
        actualIds.size() == 2
        actualIds.contains(codeSetId.toString())
        actualIds.contains(codeSet2Id.toString())

    }

    void 'test listByFolderId'() {
        given:
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())
        codeSetId = UUID.fromString(response.id as String)
        and:
        def folderResp2 = POST(FOLDERS_PATH, [label: 'Test-folder-2'])
        def folderId2 = UUID.fromString(folderResp2.id as String)
        and:
        def codeSetResp2 =POST("$FOLDERS_PATH/$folderId2$CODE_SET_PATH", codeSet())
        def codeSet2Id = UUID.fromString(codeSetResp2.id as String)

        when:
        def getByFolder2Resp = GET("$FOLDERS_PATH/$folderId2$CODE_SET_PATH")
        def getByFolderResp = GET("$FOLDERS_PATH/$folderId$CODE_SET_PATH")

        then:
        verifyAll {
            getByFolder2Resp
            getByFolder2Resp.items.id == ["$codeSet2Id"]
            getByFolderResp
            getByFolderResp.items.id == ["$codeSetId"]
        }
    }

    void 'test update CodeSet'() {
        given:
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())
        codeSetId = UUID.fromString(response.id as String)
        def newAuthor = 'New author name'
        CodeSet codeSet = codeSet()
        codeSet.author = newAuthor

        when:
        def putResponse = PUT("$CODE_SET_PATH/$codeSetId", codeSet)

        then:
        putResponse
        putResponse.author == newAuthor
        UUID.fromString(putResponse.id as String) == codeSetId
    }

    void 'add Term to CodeSet'() {
        given:
        CodeSet response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)
        codeSetId = UUID.fromString(response.id as String)
        and:
        def terminologyResponse = POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology())
        def terminologyId = UUID.fromString(terminologyResponse.id as String)
        def termResponse = POST("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH", termPayload())
        def termId = UUID.fromString(termResponse.id as String)

        when:
        def putResponse = PUT("$CODE_SET_PATH/$codeSetId$TERMS_PATH/$termId", response)

        then:
        verifyAll {
            putResponse != null
            def termGet = GET("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH/$termId")
            termGet != null
            termId == UUID.fromString(termGet.id as String)
            codeSetId == UUID.fromString(putResponse.id as String)
        }
    }

    void 'test delete codeSet with Term'() {
        given:
        def response = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())
        codeSetId = UUID.fromString(response.id as String)
        CodeSet codeSet = codeSet()
        codeSet.id = codeSetId

        and:
        def terminologyResponse = POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology())
        def terminologyId = UUID.fromString(terminologyResponse.id as String)
        def termResponse = POST("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH", termPayload())
        def termId = UUID.fromString(termResponse.id)

        PUT("$CODE_SET_PATH/$codeSetId$TERMS_PATH/$termId", codeSet)


        when:
        HttpStatus status = DELETE("$CODE_SET_PATH/$codeSetId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

    }


    def codeSet() {
        [
                label       : "Test code set",
                description : "code set description",
                author      : "A.N. Other",
                organisation: "uk.ac.gridpp.ral.org"
        ]
    }

    def folder() {
        [label: 'Test folder']
    }


    def terminology() {
        [label: 'Test terminology']
    }

    def termPayload() {
        [code: 'B15.0', definition: 'Hepatitis A with hepatic coma']
    }

}