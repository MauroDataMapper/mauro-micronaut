package org.maurodata.datamodel.diff

import org.maurodata.api.model.FieldPatchDataDTO
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeIntoDTO
import org.maurodata.api.model.ObjectPatchDataDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.diff.ArrayDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelSchemaMergeDiffsIntegrationSpec extends CommonDataSpec {
    static String NAME = 'name'
    static String CREATED = 'created'
    static String DELETED = 'deleted'

    @Shared
    UUID folderId

    @Shared
    DataModel one
    @Shared
    DataModel two
    @Shared
    DataModel ancestor
    @Shared
    DataModel ancestorAlternative
    @Shared
    DataModel branch
    @Shared
    DataModel main
    @Shared
    DataModel mainAlternative
    @Shared
    DataClass ancestorDataClass

    void setup() {
        Folder response = folderApi.create(folder())
        folderId = response.id

        this.one = dataModelApi.create(folderId, dataModelPayload())
        this.two = dataModelApi.create(folderId, dataModelPayload())

        UUID ancestorId = dataModelApi.create(folderId, dataModelPayload()).id

        this.ancestorDataClass = dataClassApi.create(ancestorId, new DataClass(
            label: 'A data class',
            description: 'description',
            minMultiplicity: -1))

        this.ancestor = dataModelApi.finalise(
            ancestorId,
            new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'ancestor'))

        UUID ancestorAlternativeId = dataModelApi.create(folderId, dataModelPayload()).id

        this.ancestorAlternative = dataModelApi.finalise(
            ancestorAlternativeId,
            new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'ancestor'))

        this.branch =
            dataModelApi.createNewBranchModelVersion(
                ancestorId,
                new CreateNewVersionData(branchName: 'branch' ))

        this.main =
            dataModelApi.createNewBranchModelVersion(
                ancestorId,
                new CreateNewVersionData(label: '1.0.1', branchName: 'main' ))

        this.mainAlternative =
            dataModelApi.createNewBranchModelVersion(
                ancestorAlternativeId,
                new CreateNewVersionData(label: '1.0.2', branchName: 'main' ))

    }

    MergeIntoDTO mergeIntoDTOFromMergeDiffDTO(MergeDiffDTO mergeDiffDTO){

        ObjectPatchDataDTO patch = new ObjectPatchDataDTO(sourceId: mergeDiffDTO.sourceId, targetId: mergeDiffDTO.targetId, label: mergeDiffDTO.label)
        mergeDiffDTO.diffs.forEach {
            FieldPatchDataDTO fieldPatchDataDTO=new FieldPatchDataDTO(
                fieldName: it.fieldName,
                path: new Path(it.path),
                sourceValue: it.sourceValue,
                targetValue: it.targetValue,
                isMergeConflict: it.isMergeConflict,
                _type: it._type
                )
            patch.patches.add(fieldPatchDataDTO)
        }

        MergeIntoDTO mergeIntoDTO=new MergeIntoDTO(patch: patch)

        return mergeIntoDTO
    }

    void 'mergediff without finalised ancestor - 1st param should throw error'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(one.id, two.id)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        String jsonString = exception.response.getBody(String).orElse(null)
        System.out.println(jsonString)
        jsonString.contains("MS01")
    }

    void 'mergediff without finalised ancestor - 2nd param should throw error'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(main.id, two.id)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        String jsonString = exception.response.getBody(String).orElse(null)
        System.out.println(jsonString)
        jsonString.contains("MS02")
    }

    void 'mergediff with finalised target - should throw error'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(ancestor.id, ancestorAlternative.id)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        String jsonString = exception.response.getBody(String).orElse(null)
        System.out.println(jsonString)
        jsonString.contains("MS03")
    }

    void 'mergediff with unrelated ancestors - should throw error'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(main.id, mainAlternative.id)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        String jsonString = exception.response.getBody(String).orElse(null)
        System.out.println(jsonString)
        jsonString.contains("MS04")
    }

    void 'mergediff on same datamodel - should show no merge diffs'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(main.id, main.id)
        then:
        mergeDiff.count == 0
    }

    void 'mergediff on newly branched datamodels - should show no merge diffs'() {
        when:
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(main.id, branch.id)
        then:
        mergeDiff.count == 0
    }

    void 'mergediff update branch datamodel label - should label update diff'() {
        when:
        DataModel updated=dataModelApi.update(branch.id, new DataModel(label:"Changed label"))
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)
        then:
        mergeDiff.diffs.forEach {
            System.out.println(it.dump())
        }
        mergeDiff.diffs.every {
            it.fieldName == 'label' && it.sourceValue == 'Changed label' && !it.isMergeConflict
        }
    }

    void 'mergediff add data class - should add a data class diff'() {
        when:

        dataClassApi.create(branch.id, dataClassPayload())
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)
        then:
        mergeDiff.diffs.every {
            it.fieldName == 'dataClasses' && it.path == 'dm:test label $main|dc:Test data class' && it._type == 'creation'
        }
    }

    void 'mergediff add data class and child class - should add a data class diff'() {
        when:

        DataClass dataClass = dataClassApi.create(branch.id, new DataClass(
            label: 'Test data class',
            description: 'other test description',
            minMultiplicity: -1))

        DataClass childDataClass = dataClassApi.create(branch.id, dataClass.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)
        then:
        // Note: even though there are two dataclasses added, this is only one merge difference
        mergeDiff.diffs.every {
            it.fieldName == 'dataClasses' && it.path == 'dm:test label $main|dc:Test data class' && it._type == 'creation'
        }
    }

    void 'mergediff modify data class - should present modify diff'() {
        when:

        ListResponse<DataClass> dataClassListResponse = dataClassApi.allDataClasses(branch.id)
        DataClass dataClass = dataClassListResponse.items.get(0)
        dataClassApi.update(branch.id, dataClass.id, new DataClass(description:"A different description"))
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        then:
        mergeDiff.diffs.every {
            it.fieldName == 'description' && it._type == 'modification'
        }
    }

    void 'mergediff delete data class - should delete diff'() {
        when:

        ListResponse<DataClass> dataClassListResponse = dataClassApi.allDataClasses(branch.id)
        DataClass dataClass = dataClassListResponse.items.get(0)

        dataClassApi.delete(branch.id, dataClass.id, dataClass)
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        then:
        mergeDiff.diffs.every {
            it.fieldName == 'dataClasses' && it.isMergeConflict && it._type == 'deletion'
        }
    }

    void 'mergediff delete data class - should not add diff'() {
        when:

        ListResponse<DataClass> dataClassListResponse = dataClassApi.allDataClasses(branch.id)
        DataClass dataClass = dataClassListResponse.items.get(0)

        dataClassApi.delete(branch.id, dataClass.id, dataClass)

        // Swap branch and main, ie merging into branch from main
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(main.id, branch.id)

        then:
        mergeDiff.count == 0
    }

    void 'mergediff add data class in one branch and delete in another - should merge conflict diff'() {
        when:

        // Add child dataclass to dataclass in main
        ListResponse<DataClass> dataClassListResponse_main = dataClassApi.allDataClasses(main.id)
        DataClass dataClass_main = dataClassListResponse_main.items.get(0)

        DataClass childDataClass = dataClassApi.create(main.id, dataClass_main.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        // Delete that dataclass in branch
        ListResponse<DataClass> dataClassListResponse = dataClassApi.allDataClasses(branch.id)
        DataClass dataClass = dataClassListResponse.items.get(0)

        dataClassApi.delete(branch.id, dataClass.id, dataClass)
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        then:
        mergeDiff.diffs.any {
            it.fieldName == 'dataClasses' && it.isMergeConflict && it._type == 'deletion' && it.path=='dm:test label $main|dc:A data class'
        }
        mergeDiff.diffs.any {
            it.fieldName == 'dataClasses' && it.isMergeConflict && it._type == 'deletion' && it.path=='dm:test label $main|dc:A data class|dc:Test child'
        }
    }

    void 'mergeinto update branch label'() {
        when:
        DataModel updated=dataModelApi.update(branch.id, new DataModel(label:"Changed label"))
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        MergeIntoDTO mergeIntoDTO=mergeIntoDTOFromMergeDiffDTO(mergeDiff)

        DataModel resultOfMerge = dataModelApi.mergeInto(mergeIntoDTO.patch.sourceId, mergeIntoDTO.patch.targetId, mergeIntoDTO)

        ObjectDiff objectDiff = dataModelApi.diffModels(branch.id, main.id)
        then:
        resultOfMerge.label == 'Changed label'
        objectDiff.diffs.every {
            it.name == 'branchName' || it.name == 'pathModelIdentifier'
        }
    }

    void 'mergeinto add data class in one branch and delete in another'() {
        when:

        // Add child dataclass to dataclass in main
        ListResponse<DataClass> dataClassListResponse_main = dataClassApi.allDataClasses(main.id)
        DataClass dataClass_main = dataClassListResponse_main.items.get(0)

        DataClass childDataClass = dataClassApi.create(main.id, dataClass_main.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        // Delete that dataclass in branch
        ListResponse<DataClass> dataClassListResponse = dataClassApi.allDataClasses(branch.id)
        DataClass dataClass = dataClassListResponse.items.get(0)

        dataClassApi.delete(branch.id, dataClass.id, dataClass)
        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        MergeIntoDTO mergeIntoDTO=mergeIntoDTOFromMergeDiffDTO(mergeDiff)

        DataModel resultOfMerge = dataModelApi.mergeInto(mergeIntoDTO.patch.sourceId, mergeIntoDTO.patch.targetId, mergeIntoDTO)

        ObjectDiff objectDiff = dataModelApi.diffModels(branch.id, main.id)

        then:
        objectDiff.diffs.every {
            it.name == 'branchName' || it.name == 'pathModelIdentifier'
        }
    }

    void 'merginto add data class and child class'() {
        when:

        DataClass dataClass = dataClassApi.create(branch.id, new DataClass(
            label: 'Test data class',
            description: 'other test description',
            minMultiplicity: -1))

        DataClass childDataClass = dataClassApi.create(branch.id, dataClass.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        MergeDiffDTO mergeDiff = dataModelApi.mergeDiff(branch.id, main.id)

        MergeIntoDTO mergeIntoDTO=mergeIntoDTOFromMergeDiffDTO(mergeDiff)

        DataModel resultOfMerge = dataModelApi.mergeInto(mergeIntoDTO.patch.sourceId, mergeIntoDTO.patch.targetId, mergeIntoDTO)



        ObjectDiff objectDiff = dataModelApi.diffModels(branch.id, main.id)

        then:
        objectDiff.diffs.every {
            it.name == 'branchName' || it.name == 'pathModelIdentifier'
        }
    }
}



