package uk.ac.ox.softeng.mauro.datamodel


import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql","classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class DataModelNewBranchVersionIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId
    @Shared
    DataModel dataModel
    @Shared
    UUID dataModelId
    @Shared
    UUID dataTypeId1
    @Shared
    UUID dataTypeId2
    @Shared
    UUID dataTypeId3
    @Shared
    UUID dataClassId1
    @Shared
    UUID dataClassId2
    @Shared
    UUID dataClassId3
    @Shared
    UUID dataElementId1
    @Shared
    UUID dataElementId2
    @Shared
    UUID enumerationTypeId

    @Shared
    UUID enumerationValueId1
    @Shared
    UUID enumerationValueId2

    @Shared
    UUID summaryMetadataId
    @Shared
    UUID summaryMetadataReportId

    @Shared
    UUID nestedReferenceFileId

    void setup() {
        folderId = folderApi.create(folder()).id
        dataModel = dataModelApi.create(folderId, dataModelPayload())
        dataModelId = dataModel.id
        dataTypeId1 = dataTypeApi.create(dataModelId,
                 new DataType(label: 'string',
                              description: 'character string of variable length',
                              dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id


        dataTypeId2 = dataTypeApi.create(dataModelId,
                 new DataType(label: 'integer',
                              description: 'a whole number, may be positive or negative, with no maximum or minimum',
                              dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)).id

        dataTypeId3 = dataTypeApi.create(dataModelId,
                new DataType(label : 'Yes/No',
                             description: 'Either a yes or a no',
                             dataTypeKind : DataType.DataTypeKind.ENUMERATION_TYPE)).id


        dataClassId1 = dataClassApi.create(dataModelId,
               new DataClass(label: 'First data class',
                             description: 'The first data class')).id

        dataClassId2 = dataClassApi.create(dataModelId,
               new DataClass(label: 'Second data class',
                             description: 'The second data class')).id

        dataClassId3 = dataClassApi.create(dataModelId, dataClassId2,
               new DataClass(label: 'Third data class',
                             description: 'The third data class')).id

        dataElementId1 = dataElementApi.create(dataModelId, dataClassId1,
               new DataElement(label: 'First data element',
                               description: 'The first data element',
                               dataType: new DataType(id: dataTypeId1))).id
        dataElementId2 = dataElementApi.create(dataModelId, dataClassId1,
               new DataElement(label: 'Second data element',
                               description: 'The second data element',
                               dataType: new DataType(id: dataTypeId2))).id


        enumerationTypeId = dataTypeApi.create(dataModelId,
               new DataType(label: 'Boolean',
                            description: 'Either true or false',
                            dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE)).id

        enumerationValueId1 = enumerationValueApi.create(dataModelId, enumerationTypeId,
               new EnumerationValue(key: 'T', value: 'True')).id

        enumerationValueId2 = enumerationValueApi.create(dataModelId, enumerationTypeId,
                new EnumerationValue(key: 'F', value: 'False')).id

        summaryMetadataId = summaryMetadataApi.create("dataModels", dataModelId,
                                                      summaryMetadataPayload()).id
        summaryMetadataReportId =
            summaryMetadataReportApi.create(
                "dataModels", dataModelId, summaryMetadataId, summaryMetadataReport()).id

        nestedReferenceFileId =
            referenceFileApi.create("dataClass", dataClassId1, referenceFilePayload()).id
    }


    void "test newBranchModelVersion -should create new datamodel and all associated objects"() {
        when:
        DataModel newBranchVersionDataModel =
            dataModelApi.createNewBranchModelVersion(
                dataModelId,
                new CreateNewVersionData(branchName: 'new branch name' ))

        then:
        newBranchVersionDataModel
        //check facets carried over
        newBranchVersionDataModel.summaryMetadata.size() == 1
        newBranchVersionDataModel.summaryMetadata[0].id != summaryMetadataId
        newBranchVersionDataModel.summaryMetadata[0].summaryMetadataReports.size() == 1
        newBranchVersionDataModel.summaryMetadata[0].summaryMetadataReports[0].id  != summaryMetadataReportId

        when:
        ListResponse<DataModel> dataModelsList = dataModelApi.list(folderId)

        then:
        dataModelsList
        dataModelsList.items.size() == 2
        [dataModelId, newBranchVersionDataModel.id] as Set == dataModelsList.items.id as Set

        when:
        ListResponse<DataClass> dataClassListResponse = dataClassApi.list(newBranchVersionDataModel.id)

        then:
        dataClassListResponse
        // There should be two child classes here
        dataClassListResponse.items.size() == 2
        // Neither should be the original classes
        dataClassListResponse.items.id.disjoint([dataClassId1, dataClassId2])
        // But should have the same names
        dataClassListResponse.items.label.sort() == ['First data class','Second data class']

        when: // One should have a child class
        UUID secondDataClassId = dataClassListResponse.items.find {it.label == 'Second data class'}.id
        UUID firstDataClassId = dataClassListResponse.items.find {it.label == 'First data class'}.id
        dataClassListResponse = dataClassApi.list(newBranchVersionDataModel.id,secondDataClassId)

        then:
        dataClassListResponse.items.size() == 1
        dataClassListResponse.items.first().label == 'Third data class'

        when:
        ListResponse<DataType> originalResp = dataTypeApi.list(dataModelId)
        then:
        originalResp
        originalResp.items.size() == 4
        originalResp.items.id as Set == [dataTypeId1, dataTypeId2, dataTypeId3,enumerationTypeId] as Set

        when:
        ListResponse<DataType> dataTypesListResponse = dataTypeApi.list(newBranchVersionDataModel.id)
        then:
        dataTypesListResponse
        dataTypesListResponse.items.size() == 4
        dataTypesListResponse.items.id.disjoint([dataTypeId1, dataTypeId2, dataTypeId3, enumerationTypeId])

        when:
        ListResponse<DataElement> dataElementListResponse =
            dataElementApi.list(newBranchVersionDataModel.id,firstDataClassId)

        then:
        dataElementListResponse

        dataElementListResponse.items.size() == 2
        dataElementListResponse.items.id.disjoint([dataElementId1, dataElementId2])
        dataElementListResponse.items.label.sort() == ['First data element', 'Second data element']

        //check nested facet in dataclass1 is cloned

        ListResponse<DataClass> newBranchModelVersionDataClasses =
            dataClassApi.list(newBranchVersionDataModel.id)
        then:
        List<UUID> newBranchModelVersionDataClassIds = newBranchModelVersionDataClasses.items.id
        DataClass retrieved = null
        when:
        newBranchModelVersionDataClassIds.find {
            retrieved = dataClassApi.show(newBranchVersionDataModel.id, it)
            !retrieved.referenceFiles.isEmpty()
        }
        then:
        retrieved.referenceFiles.size() == 1
        retrieved.referenceFiles[0].id != nestedReferenceFileId


        when:
        ObjectDiff objectDiff =
            dataModelApi.diffModels(dataModelId, newBranchVersionDataModel.id)

        then:
        objectDiff
        objectDiff.label == dataModel.label

        //branchName and path will differ
        objectDiff.diffs.name.contains(DiffBuilder.BRANCH_NAME)
        objectDiff.diffs.name.contains(DiffBuilder.PATH_MODEL_IDENTIFIER)
    }
}