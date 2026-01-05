package org.maurodata.terminology

import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class CodeSetIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID codeSetId

    def setup() {
        folderId = folderApi.create(folder()).id
    }

    void 'test post'() {
        when:
        def response = codeSetApi.create(folderId, codeSet())

        then:
        response
        response.label == "Test code set"
        response.path.toString() == 'fo:Test folder|cs:Test code set$main'
        response.description == "code set description"
        response.author == "A.N. Other"
        response.organisation == "uk.ac.gridpp.ral.org"
        response.authority
    }

    void 'test post with terms'() {
        when:
        Terminology terminology = terminologyApi.create(folderId, new Terminology(label: "My terminology"))
        Term t1 = termApi.create(terminology.id, new Term(code: "1", definition: "First"))
        termApi.create(terminology.id, new Term(code: "2", definition: "Second"))
        Term t3 = termApi.create(terminology.id, new Term(code: "3", definition: "Third"))

        CodeSet storedCodeSet = codeSetApi.create(folderId, new CodeSet(label: "My CodeSet", terms: [t1, t3]))

        ListResponse<Term> storedTerms = codeSetApi.listAllTermsInCodeSet(storedCodeSet.id)

        then:
        storedCodeSet.label == "My CodeSet"
        storedTerms.items.size() == 2
        storedTerms.items.find {it.code == "1" && it.definition == "First" && it.id == t1.id}
        storedTerms.items.find {it.code == "3" && it.definition == "Third" && it.id == t3.id}
    }


    void 'test post with terminology'() {
        when:
        Terminology terminology = terminologyApi.create(folderId, new Terminology(label: "My terminology"))
        Term t1 = termApi.create(terminology.id, new Term(code: "1", definition: "First"))
        Term t2 = termApi.create(terminology.id, new Term(code: "2", definition: "Second"))
        Term t3 = termApi.create(terminology.id, new Term(code: "3", definition: "Third"))

        CodeSet storedCodeSet = codeSetApi.create(folderId, new CodeSet(label: "My CodeSet", terminologies: [terminology]))

        ListResponse<Term> storedTerms = codeSetApi.listAllTermsInCodeSet(storedCodeSet.id)

        then:
        storedCodeSet.label == "My CodeSet"
        storedTerms.items.size() == 3
        storedTerms.items.find {it.code == "1" && it.definition == "First" && it.id == t1.id}
        storedTerms.items.find {it.code == "2" && it.definition == "Second" && it.id == t2.id}
        storedTerms.items.find {it.code == "3" && it.definition == "Third" && it.id == t3.id}
    }


    void 'test codeSet getById'() {
        given:
        def codeSetPayload = codeSet()
        def response = codeSetApi.create(folderId, codeSetPayload)
        codeSetId = response.id

        when:
        CodeSet getResponse = codeSetApi.show(codeSetId)

        then:
        getResponse
        getResponse.label == codeSetPayload.label
        getResponse.description == codeSetPayload.description
        getResponse.organisation == codeSetPayload.organisation
        getResponse.author == codeSetPayload.author
        getResponse.authority
    }

    void 'test codeSet listAll'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id

        and:
        Folder folderResp2 = folderApi.create(new Folder(label: 'Test-folder-2'))
        UUID folderId2 = folderResp2.id
        and:
        CodeSet codeSetResp2 = codeSetApi.create(folderId2, codeSet())
        UUID codeSet2Id = codeSetResp2.id

        when:
        ListResponse<CodeSet> getAllResp = codeSetApi.listAll()

        then:
        getAllResp != null
        List<UUID> actualIds = getAllResp.items.id
        actualIds.size() == 2
        actualIds.contains(codeSetId)
        actualIds.contains(codeSet2Id)

    }

    void 'test listByFolderId'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        and:
        Folder folderResp2 = folderApi.create(new Folder(label: 'Test-folder-2'))
        UUID folderId2 = folderResp2.id
        and:
        CodeSet codeSetResp2 = codeSetApi.create(folderId2, codeSet())
        UUID codeSet2Id = codeSetResp2.id

        when:
        ListResponse<CodeSet> getByFolder2Resp = codeSetApi.list(folderId2)
        ListResponse<CodeSet> getByFolderResp =  codeSetApi.list(folderId)

        then:
        getByFolder2Resp
        getByFolder2Resp.items.id == [codeSet2Id]
        getByFolderResp
        getByFolderResp.items.id == [codeSetId]

    }

    void 'test list multiple codesets ByFolderId'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id

        when:
        ListResponse<CodeSet> getByFolderResp =  codeSetApi.list(folderId)

        then:
        getByFolderResp.items.id == [codeSetId]

        when:
        CodeSet codeSet2 = codeSetApi.create(folderId, codeSet())
        UUID codeSet2Id = codeSet2.id
        ListResponse<CodeSet> folderResponse2 =  codeSetApi.list(folderId)

        then:
        folderResponse2.items.id.sort() == [codeSetId, codeSet2Id].sort()
    }

    void 'test update CodeSet'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        def newAuthor = 'New author name'
        CodeSet codeSet = codeSet()
        codeSet.author = newAuthor

        when:
        CodeSet putResponse = codeSetApi.update(codeSetId, codeSet)

        then:
        putResponse
        putResponse.author == newAuthor
        putResponse.id == codeSetId
        putResponse.authority
    }

    void 'add Term to CodeSet'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        and:
        Terminology terminologyResponse = terminologyApi.create(folderId, terminologyPayload())
        UUID terminologyId = terminologyResponse.id
        Term termResponse = termApi.create(terminologyId, termPayload())
        UUID termId = termResponse.id

        when:
        CodeSet putResponse = codeSetApi.addTerm(codeSetId, termId)

        then:
        putResponse
        Term termGet = termApi.show(terminologyId, termId)
        termGet
        termId == termGet.id
        codeSetId == putResponse.id
    }

    void 'test delete codeSet with Term'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id

        CodeSet codeSet = codeSet()
        codeSet.id = codeSetId

        ReferenceFile referenceFile = referenceFileApi.create("codeSet", codeSetId, referenceFilePayload())

        and:
        Terminology terminologyResponse = terminologyApi.create(folderId, terminologyPayload())
        UUID terminologyId = terminologyResponse.id
        Term termResponse = termApi.create(terminologyId, termPayload())
        UUID termId = termResponse.id

        codeSetApi.addTerm(codeSetId, termId)
        //verify
        CodeSet codeSetWithTerm = codeSetApi.show(codeSetId)
        codeSetWithTerm.id == codeSetId

        when:
        HttpResponse deleteResponse = codeSetApi.delete(codeSetId, new CodeSet(),true)

        then:
        deleteResponse.status == HttpStatus.NO_CONTENT

        when:
        ListResponse<CodeSet> codeSets = codeSetApi.listAll()

        then:
        List<UUID> codeSetIds = codeSets.items.id.collect()
        !codeSetIds.contains(codeSetId)

        when:
        ReferenceFile refResp = referenceFileApi.show("codeSet", codeSetId, referenceFile.id)
        then:
        !refResp
    }

    void 'test getTermsForCodeSet'() {
        given:
        def response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        CodeSet codeSet = codeSet() as CodeSet
        codeSet.id = codeSetId

        and:
        Terminology terminologyResponse = terminologyApi.create(folderId, terminologyPayload())
        UUID terminologyId = terminologyResponse.id
        Term termResponse = termApi.create(terminologyId, termPayload())
        UUID termId = termResponse.id

        //Associating term to codeSet
        codeSetApi.addTerm(codeSetId, termId)

        when:
        ListResponse<Term> getAllResp = codeSetApi.listAllTermsInCodeSet(codeSetId)

        then:
        getAllResp
        List<Term> terms = getAllResp.items
        terms.size() == 1
        terms.id.contains(termId)
        terms.domainType.contains('Term')
    }

    void 'test getTermsForCodeSet multiple terms'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = UUID.fromString(response.id as String)
        CodeSet codeSet = codeSet() as CodeSet
        codeSet.id = codeSetId

        and:
        Terminology terminologyResponse = terminologyApi.create(folderId, terminologyPayload())
        UUID terminologyId = terminologyResponse.id

        Term termResponse1 = termApi.create(terminologyId, termPayload())
        UUID termId1 = termResponse1.id

        Term termResponse2 = termApi.create(terminologyId, new Term(code: 'code', definition: 'a defiintion'))
        UUID termId2 = termResponse2.id

        //Associating term to codeSet
        codeSetApi.addTerm(codeSetId, termId1)
        codeSetApi.addTerm(codeSetId, termId2)

        when:
        ListResponse<Term> getAllResp = codeSetApi.listAllTermsInCodeSet(codeSetId)

        then:
        getAllResp
        List<Term> terms = getAllResp.items
        terms.size() == 2
        terms.id.contains(termId1)
        terms.id.contains(termId2)
    }

    void 'test getTermsForCodeSet -no associated terms should return empty list '() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        CodeSet codeSet = codeSet() as CodeSet
        codeSet.id = codeSetId

        when:
        ListResponse<Term> getAllResp = codeSetApi.listAllTermsInCodeSet(codeSetId)

        then:
        getAllResp
        getAllResp.items.size() == 0

    }

    void 'test remove Term from codeSet'() {
        given:
        CodeSet response = codeSetApi.create(folderId, codeSet())
        codeSetId = response.id
        CodeSet codeSet = codeSet() as CodeSet
        codeSet.id = codeSetId

        and:
        Terminology terminologyResponse = terminologyApi.create(folderId, terminologyPayload())
        UUID terminologyId = terminologyResponse.id
        Term termResponse = termApi.create(terminologyId, termPayload())
        UUID termId = termResponse.id

        //Associating term to codeSet
        codeSetApi.addTerm(codeSetId, termId)
        //verify using codeset/terms endpoint
        when:
        ListResponse<Term> getTermsForCodeSet = codeSetApi.listAllTermsInCodeSet(codeSetId)

        then:
        getTermsForCodeSet
        getTermsForCodeSet.items.size() == 1
        getTermsForCodeSet.items.id.contains(termId)

        when:
        CodeSet updated = codeSetApi.removeTermFromCodeSet(codeSetId, termId)

        then:
        updated
        updated.id == codeSetId

        //verify term is no longer associated with codeset
        when:
        ListResponse<Term> getTermsForCodeSetResp = codeSetApi.listAllTermsInCodeSet(codeSetId)

        then:
        getTermsForCodeSetResp
        getTermsForCodeSetResp.items.size() == 0

        //Verify both term and codeSet exist after removing link between term and codeSet.
        when:
        Term existing = termApi.show(terminologyId, termId)
        CodeSet updateCodeSet = codeSetApi.show(codeSetId)

        then:
        existing
        existing.id == termId
        updateCodeSet
        updateCodeSet.id == codeSetId
    }

}