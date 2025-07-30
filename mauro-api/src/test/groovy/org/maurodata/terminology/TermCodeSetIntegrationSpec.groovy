package org.maurodata.terminology

import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.web.ListResponse
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class TermCodeSetIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId

    @Shared
    UUID termId

    def setup() {
        Folder folderResponse = folderApi.create(folder())
        folderId = folderResponse.id
        Terminology terminologyResp = terminologyApi.create(folderId, terminologyPayload())
        terminologyId = terminologyResp.id

        Term termResponse = termApi.create(terminologyId, termPayload())
        termId = termResponse.id
    }

    void 'test getCodeSetsForTerm'() {
        given:
        CodeSet codeSetResp = codeSetApi.create(folderId, codeSet())
        UUID codeSetId = codeSetResp.id
        and:
        //Associating term to codeSet
        codeSetApi.addTerm(codeSetId, termId)

        when:
        ListResponse<CodeSet> getAllResp = termApi.getCodeSetsForTerm(terminologyId, termId)

        then:
        getAllResp
        List<CodeSet> codeSets = getAllResp.items
        codeSets.size() == 1
        codeSets.id == [codeSetId]
        codeSets.domainType.contains('CodeSet')
    }

    void 'test getCodeSetsByTerm -no association -returns  empty list '() {
        given:
        codeSetApi.create(folderId, codeSet())

        when:
        ListResponse<CodeSet> getAllResp = termApi.getCodeSetsForTerm(terminologyId, termId)

        then:
        getAllResp
        getAllResp.items.size() == 0

    }


}