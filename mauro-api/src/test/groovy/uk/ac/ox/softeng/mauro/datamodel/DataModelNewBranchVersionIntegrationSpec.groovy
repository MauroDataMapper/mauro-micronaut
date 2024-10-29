package uk.ac.ox.softeng.mauro.datamodel

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.*
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql","classpath:sql/tear-down.sql"], phase = Sql.Phase.AFTER_EACH)
class DataModelNewBranchVersionIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

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
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        dataModel = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)
        dataModelId = dataModel.id
        dataTypeId1 = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'string', description: 'character string of variable length', domainType: 'PrimitiveType'], DataType)).id


        dataTypeId2 = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", [label: 'integer', description: 'a whole number, may be positive or negative, with no maximum or minimum', domainType: 'PrimitiveType'], DataType)).id

        dataTypeId3 = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH",
                [label      : 'Yes/No',
                 description: 'Either a yes or a no',
                 domainType : 'EnumerationType'], DataType)).id


        dataClassId1 = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'First data class', description: 'The first data class'], DataClass)).id
        dataClassId2 = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'Second data class', description: 'The second data class'], DataClass)).id
        dataClassId3 = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId2$DATACLASSES_PATH", [label: 'Third data class', description: 'The third data class'], DataClass)).id

        dataElementId1 = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId1$DATA_ELEMENTS_PATH", [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId1]], DataElement)).id
        dataElementId2 = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId1$DATA_ELEMENTS_PATH", [label: 'Second data element', description: 'The second data element', dataType: [id: dataTypeId2]], DataElement)).id


        enumerationTypeId =  ((DataType) POST("/dataModels/$dataModelId/dataTypes", [label: 'Boolean', description: 'Either true or false',domainType: 'EnumerationType'], DataType)).id
        enumerationValueId1 = ((EnumerationValue) POST("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", [key: 'T', value: 'True'], EnumerationValue)).id

        enumerationValueId2 = ((EnumerationValue) POST("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", [key: 'F', value: 'False'], EnumerationValue)).id

        summaryMetadataId = ((SummaryMetadata) POST("$DATAMODELS_PATH/$dataModelId$SUMMARY_METADATA_PATH", summaryMetadataPayload(), SummaryMetadata)).id
        summaryMetadataReportId = ((SummaryMetadataReport) POST("$DATAMODELS_PATH/$dataModelId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH", summaryMetadataReport(), SummaryMetadataReport)).id

        nestedReferenceFileId = ((ReferenceFile) POST("$DATACLASSES_PATH/$dataClassId1$REFERENCE_FILE_PATH", referenceFilePayload(), ReferenceFile)).id
    }


    void "test newBranchModelVersion -should create new datamodel and all associated objects"() {
        when:
        DataModel newBranchVersionDataModel = (DataModel) PUT("$DATAMODELS_PATH/$dataModelId$NEW_BRANCH_MODEL_VERSION", [branchName: 'new branch name'], DataModel)

        then:
        newBranchVersionDataModel
        //check facets carried over
        newBranchVersionDataModel.summaryMetadata.size() == 1
        newBranchVersionDataModel.summaryMetadata[0].id != summaryMetadataId
        newBranchVersionDataModel.summaryMetadata[0].summaryMetadataReports.size() == 1
        newBranchVersionDataModel.summaryMetadata[0].summaryMetadataReports[0].id  != summaryMetadataReportId

        when:
        ListResponse<DataModel> dataModelsList = (ListResponse<DataModel>) GET("$FOLDERS_PATH/$folderId/$DATAMODELS_PATH", ListResponse, DataModel)

        then:
        dataModelsList
        dataModelsList.items.size() == 2
        dataModelsList.items.collect { it.id.toString() }.sort() == [dataModelId, newBranchVersionDataModel.id].collect { it.toString() }.sort()

        when:
        ListResponse<DataClass> dataClassListResponse = (ListResponse<DataClass>) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATACLASSES_PATH", ListResponse, DataClass)

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
        dataClassListResponse = (ListResponse<DataClass>) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATACLASSES_PATH/${secondDataClassId.toString()}/dataClasses", ListResponse, DataClass)

        then:
        dataClassListResponse.items.size() == 1
        dataClassListResponse.items.first().label == 'Third data class'

        when:
        ListResponse<DataType> originalResp = (ListResponse<DataType>) GET("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", ListResponse, DataType)
        then:
        originalResp
        originalResp.items.size() == 4
        originalResp.items.id.sort() == [dataTypeId1, dataTypeId2, dataTypeId3,enumerationTypeId].sort()

        when:
        ListResponse<DataType> dataTypesListResponse = (ListResponse<DataType>) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATATYPES_PATH", ListResponse, DataType)
        then:
        dataTypesListResponse
        dataTypesListResponse.items.size() == 4
        dataTypesListResponse.items.id.disjoint([dataTypeId1, dataTypeId2, dataTypeId3, enumerationTypeId])

        when:
        ListResponse<DataElement> dataElementListResponse = (ListResponse<DataElement>) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATACLASSES_PATH/${firstDataClassId.toString()}/dataElements", ListResponse, DataElement)

        then:
        dataElementListResponse

        dataElementListResponse.items.size() == 2
        dataElementListResponse.items.id.disjoint([dataElementId1, dataElementId2])
        dataElementListResponse.items.label.sort() == ['First data element', 'Second data element']

        //check nested facet in dataclass1 is cloned

        ListResponse<DataClass> newBranchModelVersionDataClasses = (ListResponse<DataClass>) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATACLASSES_PATH",ListResponse, DataClass)
        then:
        List<UUID> newBranchModelVersionDataClassIds = newBranchModelVersionDataClasses.items.id
        DataClass retrieved = null
        when:
        newBranchModelVersionDataClassIds.find {
            retrieved = (DataClass) GET("$DATAMODELS_PATH/$newBranchVersionDataModel.id$DATACLASSES_PATH/$it", DataClass)
            !retrieved.referenceFiles.isEmpty()
        }
        then:
        retrieved.referenceFiles.size() == 1
        retrieved.referenceFiles[0].id != nestedReferenceFileId


        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$dataModelId$DIFF/$newBranchVersionDataModel.id", Map<String, Object>)

        then:
        diffMap
        diffMap.get(DiffBuilder.LABEL) == dataModel.label
        diffMap.diffs.each { [DiffBuilder.BRANCH_NAME,  DiffBuilder.PATH_MODEL_IDENTIFIER].contains(it.name) }
        //branchName and path will differ
        diffMap.count == 2
    }
}