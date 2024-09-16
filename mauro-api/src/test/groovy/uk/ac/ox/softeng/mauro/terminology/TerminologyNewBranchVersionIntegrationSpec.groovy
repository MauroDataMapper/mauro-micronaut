package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.*
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class TerminologyNewBranchVersionIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

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
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        codeSet = (CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)
        terminologyId = ((Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology(), Terminology)).id
        termId1 = ((Term) POST("$TERMINOLOGIES_PATH/$terminologyId/terms", [code: 'TEST-1', definition: 'first term'], Term)).id
        termId2 = ((Term) POST("$TERMINOLOGIES_PATH/$terminologyId/terms", term(), Term)).id
        termRelationshipTypeId  =((TermRelationshipType) POST("$TERMINOLOGIES_PATH/$terminologyId$TERM_RELATIONSHIP_TYPES", [label: 'Test relationship type', childRelationship: true], TermRelationshipType)).id
        termRelationshipId = ((TermRelationship) POST("$TERMINOLOGIES_PATH/$terminologyId$TERM_RELATIONSHIP_PATH", [
                relationshipType: [id: termRelationshipTypeId],
                sourceTerm: [id: termId1],
                targetTerm: [id: termId2]
        ], TermRelationship)).id
        annotation = (Annotation) POST("$TERMINOLOGIES_PATH/$terminologyId$ANNOTATION_PATH", annotationPayload(), Annotation)
        childAnnotation = (Annotation) POST("$TERMINOLOGIES_PATH/$terminologyId$ANNOTATION_PATH/$annotation.id$ANNOTATION_PATH", annotationPayload('child annotation label', 'test child description'), Annotation)
        referenceFileId = ((ReferenceFile) POST("$TERMINOLOGIES_PATH/$terminologyId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)).id

    }


    void 'test new branch model version - codeSet should be cloned, with links in original codeSet'() {
        given:
        //Associating term to codeSet
        PUT("$CODE_SET_PATH/$codeSet.id$TERMS_PATH/$termId1", codeSet, CodeSet)
        PUT("$CODE_SET_PATH/$codeSet.id$TERMS_PATH/$termId2", codeSet, CodeSet)

        when:
        CodeSet newBranchVersionCodeSet = (CodeSet) PUT("$CODE_SET_PATH/$codeSet.id/$NEW_BRANCH_MODEL_VERSION", [branchName: 'new branch name'], CodeSet)

        then:
        newBranchVersionCodeSet

        when:
        ListResponse<Term> termsByCodeSetList = (ListResponse<Term>)  GET("$CODE_SET_PATH/$newBranchVersionCodeSet.id$TERMS_PATH", ListResponse<Term>)
        then:
        termsByCodeSetList
        termsByCodeSetList.items.size() == 2
        termsByCodeSetList.items.id.sort().containsAll([termId1.toString(), termId2.toString()].sort())
    }


    void 'test new branch model version - terminology -all related objects should be cloned and persisted'() {
        when:
        Terminology newBranchVersionTerminology = (Terminology) PUT("$TERMINOLOGIES_PATH/$terminologyId/$NEW_BRANCH_MODEL_VERSION", [branchName: 'new branch name'], Terminology)

        then:
        newBranchVersionTerminology
        newBranchVersionTerminology.id != terminologyId
        when:
        ListResponse<Terminology> terminologies = (ListResponse<Terminology>)  GET("$FOLDERS_PATH/$folderId/$TERMINOLOGIES_PATH", ListResponse<Terminology>)
        then:
        terminologies
        terminologies.items.size() == 2
        terminologies.items.id.sort().containsAll([terminologyId.toString(), newBranchVersionTerminology.id.toString()].sort())

        Terminology newBranchVersion = (Terminology) GET("$TERMINOLOGIES_PATH/$newBranchVersionTerminology.id", Terminology)
        newBranchVersion.referenceFiles.size() == 1
        newBranchVersion.referenceFiles[0].id != referenceFileId

        when:
        ListResponse<Term> newTerms = (ListResponse<Term>)  GET("$TERMINOLOGIES_PATH/$newBranchVersionTerminology.id$TERMS_PATH", ListResponse<Term>)
        then:
        newTerms
        List<UUID> newTermsIdsList = newTerms.items.id
        newTermsIdsList.disjoint([termId1, termId2])

        when:
        ListResponse<TermRelationshipType> termRelationshipTypes =  (ListResponse<TermRelationshipType>)  GET("$TERMINOLOGIES_PATH/$newBranchVersionTerminology.id$TERM_RELATIONSHIP_TYPES", ListResponse<TermRelationshipType>)
        then:
        termRelationshipTypes

        termRelationshipTypes.items.size() == 1
        termRelationshipTypes.items[0].id != termRelationshipTypeId

        when:
        ListResponse<TermRelationship> termRelationship =  (ListResponse<TermRelationship>)  GET("$TERMINOLOGIES_PATH/$newBranchVersionTerminology.id$TERM_RELATIONSHIP_PATH", ListResponse<TermRelationship>)
        then:
        termRelationship

        termRelationship.items.size() == 1
        termRelationship.items[0].id != termRelationshipId

        when:
        ListResponse<Annotation> annotations =  (ListResponse<Annotation>)  GET("$TERMINOLOGIES_PATH/$newBranchVersionTerminology.id$ANNOTATION_PATH", ListResponse<Annotation>)
        then:
        annotations
        annotations.items.size() == 1
        annotations.items[0].id != annotation.id
        annotations.items[0].childAnnotations.size() == 1
        annotations.items[0].childAnnotations[0].id != childAnnotation.id

        when:
        Map<String, Object> diffMap = GET("$TERMINOLOGIES_PATH/$terminologyId$DIFF/$newBranchVersionTerminology.id", Map<String, Object>)

        then:
        diffMap
        //branchName and path will differ
        diffMap.diffs.each { [DiffBuilder.BRANCH_NAME, DiffBuilder.PATH_MODEL_IDENTIFIER].contains(it.name) }
        diffMap.count == 2
    }

}