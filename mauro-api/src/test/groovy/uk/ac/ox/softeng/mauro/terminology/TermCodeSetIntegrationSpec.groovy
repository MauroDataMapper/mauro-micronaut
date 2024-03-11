package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class TermCodeSetIntegrationSpec extends CommonDataSpec {

    @Inject
    @AutoCleanup
    @Shared
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId

    @Shared
    UUID termId

    def setup() {
        def folderResponse = POST(FOLDERS_PATH, folder())
        folderId = UUID.fromString(folderResponse.id as String)
        def terminologyResp = POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology())
        terminologyId = UUID.fromString(terminologyResp.id as String)

        def termResponse = POST("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH", termPayload())
        termId = UUID.fromString(termResponse.id as String)
    }

    void 'test getCodeSetsForTerm'() {
        given:
        def codeSetResp = POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())
        def codeSetId = UUID.fromString(codeSetResp.id as String)
        and:
        //Associating term to codeSet
        PUT("$CODE_SET_PATH/$codeSetId$TERMS_PATH/$termId", codeSetResp, CodeSet)

        when:
        def getAllResp = GET("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH/$termId$CODE_SET_PATH")

        then:
        getAllResp
        def codeSets = getAllResp.items
        codeSets.size() == 1
        codeSets.id.contains(codeSetId.toString())
        codeSets.domainType.contains('CodeSet')
    }

    void 'test getCodeSetsByTerm -no association -returns  empty list '() {
        given:
        POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet())

        when:
        def getAllResp = GET("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH/$termId$CODE_SET_PATH")

        then:
        getAllResp
        getAllResp.items.size() == 0

    }


}