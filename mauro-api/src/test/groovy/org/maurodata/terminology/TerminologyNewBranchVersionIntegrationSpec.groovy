package org.maurodata.terminology

import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.version.CreateNewVersionData

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.*
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class TerminologyNewBranchVersionIntegrationSpec extends CommonDataSpec {

    @Shared
    CodeSet codeSet

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId
    @Shared
    UUID termId1
    @Shared
    UUID termId2
    @Shared
    UUID termRelationshipTypeId
    @Shared
    UUID termRelationshipId

    @Shared
    Annotation annotation
    @Shared
    Annotation childAnnotation
    @Shared
    UUID  referenceFileId

    void setup() {
        folderId = folderApi.create(folder()).id
        codeSet = codeSetApi.create(folderId, codeSet())
        terminologyId = terminologyApi.create(folderId, terminology()).id
        termId1 = termApi.create(terminologyId, new Term(code: 'TEST-1', definition: 'first term')).id
        termId2 = termApi.create(terminologyId, term()).id
        termRelationshipTypeId = termRelationshipTypeApi.create(
            terminologyId, new TermRelationshipType(label: 'Test relationship type', childRelationship: true)).id
        termRelationshipId = termRelationshipApi.create(terminologyId, new TermRelationship(
                relationshipType: new TermRelationshipType(id: termRelationshipTypeId),
                sourceTerm: new Term(id: termId1),
                targetTerm: new Term(id: termId2))).id
        annotation = annotationApi.create("terminology", terminologyId, annotationPayload())
        childAnnotation = annotationApi.create(
            "terminology", terminologyId, annotation.id,
            annotationPayload('child annotation label', 'test child description'))
        referenceFileId = referenceFileApi.create("terminology", terminologyId, referenceFilePayload()).id

    }


    void 'test new branch model version - codeSet should be cloned, with links in original codeSet'() {
        given:
        //Associating term to codeSet
        codeSetApi.addTerm(codeSet.id,termId1)
        codeSetApi.addTerm(codeSet.id,termId2)

        when:
        CodeSet newBranchVersionCodeSet = codeSetApi.createNewBranchModelVersion(
            codeSet.id, new CreateNewVersionData(branchName: 'new branch name'))

        then:
        newBranchVersionCodeSet

        when:
        ListResponse<Term> termsbyCodeSetList = codeSetApi.listAllTermsInCodeSet(newBranchVersionCodeSet.id)
        then:
        termsbyCodeSetList
        termsbyCodeSetList.items.size() == 2
        termsbyCodeSetList.items.id.sort() == [termId1, termId2].sort()
    }


    void 'test new branch model version - terminology -all related objects should be cloned and persisted'() {
        when:
        Terminology newBranchVersionTerminology = terminologyApi.createNewBranchModelVersion(
            terminologyId, new CreateNewVersionData(branchName: 'new branch name'))

        then:
        newBranchVersionTerminology
        newBranchVersionTerminology.id != terminologyId
        when:
        ListResponse<Terminology> terminologies = terminologyApi.list(folderId)
        then:
        terminologies
        terminologies.items.size() == 2
        terminologies.items.id.sort() == [terminologyId, newBranchVersionTerminology.id].sort()

        Terminology newBranchVersion = terminologyApi.show(newBranchVersionTerminology.id)
        newBranchVersion.referenceFiles.size() == 1
        newBranchVersion.referenceFiles[0].id != referenceFileId

        when:
        ListResponse<Term> newTerms = termApi.list(newBranchVersionTerminology.id)
        then:
        newTerms
        List<UUID> newTermsIdsList = newTerms.items.id
        newTermsIdsList.disjoint([termId1, termId2])

        when:
        ListResponse<TermRelationshipType> termRelationshipTypes =  termRelationshipTypeApi.list(newBranchVersionTerminology.id)
        then:
        termRelationshipTypes

        termRelationshipTypes.items.size() == 1
        termRelationshipTypes.items[0].id != termRelationshipTypeId

        when:
        ListResponse<TermRelationship> termRelationship = termRelationshipApi.list(newBranchVersionTerminology.id)
        then:
        termRelationship

        termRelationship.items.size() == 1
        termRelationship.items[0].id != termRelationshipId

        when:
        ListResponse<Annotation> annotations =  annotationApi.list("terminology", newBranchVersionTerminology.id)
        then:
        annotations
        annotations.items.size() == 1
        annotations.items[0].id != annotation.id
        annotations.items[0].childAnnotations.size() == 1
        annotations.items[0].childAnnotations[0].id != childAnnotation.id

        when:
        ObjectDiff diffMap = terminologyApi.diffModels(terminologyId, newBranchVersionTerminology.id)

        then:
        diffMap
        //branchName and path will differ
        diffMap.diffs.each { [DiffBuilder.BRANCH_NAME, DiffBuilder.PATH_MODEL_IDENTIFIER].contains(it.name) }
        diffMap.numberOfDiffs == 2
    }

}