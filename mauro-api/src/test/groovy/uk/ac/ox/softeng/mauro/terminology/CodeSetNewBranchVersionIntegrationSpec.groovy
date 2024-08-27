package uk.ac.ox.softeng.mauro.terminology


import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class CodeSetNewBranchVersionIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application
    @Shared
    UUID folderId
    @Shared
    UUID terminologyId
    @Shared
    UUID termId
    @Shared
    UUID referenceFileId
    @Shared
    UUID codeSetId


    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        codeSetId = ((CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", codeSet(), CodeSet)).id

        terminologyId = ((Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", terminology(), Terminology)).id

        termId = ((Term) POST("$TERMINOLOGIES_PATH/$terminologyId$TERMS_PATH", termPayload(), Term)).id
        //add term to codeSet
        (CodeSet) PUT("$CODE_SET_PATH/$codeSetId$TERMS_PATH/$termId", null, CodeSet)

        referenceFileId = ((ReferenceFile) POST ("$CODE_SET_PATH/$codeSetId$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)).id
    }

    void "test newBranchModelVersion -should clone new codeSet and facets (administered items), keeping original associations (terms)"() {
        when:
        CodeSet newBranchVersion = (CodeSet) PUT("$CODE_SET_PATH/$codeSetId$NEW_BRANCH_MODEL_VERSION", [branchName: 'new branch name'], CodeSet)

        then:
        newBranchVersion
        //check facets carried over
        newBranchVersion.referenceFiles.size() == 1
        newBranchVersion.referenceFiles[0].id != referenceFileId

        when:
        Map<String, Object> diffMap = GET("$CODE_SET_PATH/$codeSetId$DIFF/$newBranchVersion.id", Map<String, Object>)

        then:
        diffMap
        //branchName and path will differ
        diffMap.diffs.each { [DiffBuilder.BRANCH_NAME,  DiffBuilder.PATH_MODEL_IDENTIFIER].contains(it.name) }
        diffMap.count == 2

    }


}