package org.maurodata.folder

import jakarta.inject.Singleton
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeIntoDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
class VersionedFolderMergeIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID storedBaseFolderId

    @Shared
    UUID storedBranch1FolderId

    @Shared
    UUID storedBranch2FolderId

    @Shared
    Folder baseFolder = Folder.build {
        finalised true
        modelVersion '1.0.0'
        modelVersionTag 'BaseVersion'
        label 'Test Merge Folder'
        dataModel {
            label 'My first dataModel'
            dataClass {
                label 'My first dataClass'
            }
        }
        terminology {
            label 'My first Terminology'
            term {
                code 'Y'
                definition 'Yes!'
            }
            term {
                code 'N'
                definition 'Definitely not!'
            }
        }
    }

    void setupSpec() {
        Folder folder = folderApi.create(new Folder(label: 'Test'))
        //        folderApi.delete(folder.id, new Folder(id: folder.id), true)
        //        folder = findOrCreateFolderByLabel('Test')
        storedBaseFolderId = importFolder(baseFolder, folder)
        Folder mainFolder = versionedFolderApi.createNewBranchModelVersion(storedBaseFolderId, new CreateNewVersionData())
        storedBranch1FolderId = mainFolder.id
        Folder branchFolder = versionedFolderApi.
            createNewBranchModelVersion(storedBaseFolderId, new CreateNewVersionData(branchName: 'My New Branch'))

        storedBranch2FolderId = branchFolder.id
        ListResponse<DataModel> branchDataModels = dataModelApi.list(storedBranch2FolderId)
        ListResponse<Terminology> branchTerminologies = terminologyApi.list(storedBranch2FolderId)
        ListResponse<Terminology> mainTerminologies = terminologyApi.list(storedBranch2FolderId)

        // delete something in the branch
        ListResponse<DataClass> branchDataClasses = dataClassApi.list(branchDataModels.items.first().id)
        dataClassApi.delete(branchDataModels.items.first().id, branchDataClasses.items.first().id, new DataClass())

        // add something in the branch
        dataModelApi.create(storedBranch2FolderId, new DataModel(
            label: 'My second DataModel'
        ))

        // modify something in one branch
        ListResponse<Term> branchTerms = termApi.list(branchTerminologies.items.first().id)
        Term branchYTerm = branchTerms.items.find {it.code == 'Y'}
        termApi.update(branchTerminologies.items.first().id, branchYTerm.id, new Term(code: 'Y', definition: 'Maybe!'))

        // modify something in both branches
        Term branchNTerm = branchTerms.items.find {it.code == 'N'}
        termApi.update(branchTerminologies.items.first().id, branchNTerm.id, new Term(code: 'N', definition: 'Definitely Yes!'))

        ListResponse<Term> mainTerms = termApi.list(mainTerminologies.items.first().id)
        Term mainNTerm = mainTerms.items.find {it.code == 'N'}
        termApi.update(mainTerminologies.items.first().id, mainNTerm.id, new Term(code: 'N', definition: 'Definitely No!'))

    }


    void "Test merge on folders"() {

        when:
        MergeDiffDTO mergeDiffDTO = versionedFolderApi.mergeDiff(storedBranch2FolderId, storedBranch1FolderId)
        then:

        mergeDiffDTO.sourceId == storedBranch2FolderId
        mergeDiffDTO.targetId == storedBranch1FolderId

        mergeDiffDTO.diffs.size() == 4

        // MergeIntoDTO mergeIntoDTO = new


        // versionedFolderApi.mergeInto(storedBranch2FolderId, storedBranch1FolderId, )

    }


}
