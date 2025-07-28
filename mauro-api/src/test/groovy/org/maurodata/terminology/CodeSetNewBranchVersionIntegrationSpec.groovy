package org.maurodata.terminology

import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.version.CreateNewVersionData
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class CodeSetNewBranchVersionIntegrationSpec extends CommonDataSpec {

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
        folderId = folderApi.create(folder()).id
        codeSetId = codeSetApi.create(folderId, codeSet()).id

        terminologyId = terminologyApi.create(folderId, terminologyPayload()).id

        termId = termApi.create(terminologyId, termPayload()).id
        //add term to codeSet
        codeSetApi.addTerm(codeSetId, termId)

        referenceFileId = referenceFileApi.create("codeSet", codeSetId, referenceFilePayload()).id
    }

    void "test newBranchModelVersion -should clone new codeSet and facets (administered items), keeping original associations (terms)"() {
        when:
        CodeSet newBranchVersion = codeSetApi.createNewBranchModelVersion(
            codeSetId, new CreateNewVersionData(branchName: 'new branch name'))

        then:
        newBranchVersion
        //check facets carried over
        newBranchVersion.referenceFiles.size() == 1
        newBranchVersion.referenceFiles[0].id != referenceFileId

        when:
        ObjectDiff objectDiff = codeSetApi.diffModels(codeSetId, newBranchVersion.id)

        then:
        objectDiff
        //branchName and path will differ
        objectDiff.diffs.each { [DiffBuilder.BRANCH_NAME,  DiffBuilder.PATH_MODEL_IDENTIFIER].contains(it.name) }
        objectDiff.numberOfDiffs == 2

    }


}