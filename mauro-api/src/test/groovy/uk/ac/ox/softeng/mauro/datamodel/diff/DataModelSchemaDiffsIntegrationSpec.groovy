package uk.ac.ox.softeng.mauro.datamodel.diff

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff
import uk.ac.ox.softeng.mauro.domain.diff.BaseCollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelSchemaDiffsIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId
    @Shared
    UUID leftDataModelId

    @Shared
    UUID rightDataModelId

    void setup() {
        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        DataModel leftResponse = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)
        leftDataModelId = leftResponse.id

        DataModel rightResponse = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH",
                [label: 'Test data model', description: 'right description', author: ' right test author'], DataModel)
        rightDataModelId = rightResponse.id
    }


    void 'diff dataModels with same data class - should have no differences'() {
        given:
        //modified comparison key = label
        (DataClass) POST("$DATAMODELS_PATH/$leftDataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)

        (DataClass) POST("$DATAMODELS_PATH/$rightDataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)

        when:
        ObjectDiff diff = (ObjectDiff) GET("$DATAMODELS_PATH/$leftDataModelId/diff/$rightDataModelId", ObjectDiff)

        then:
        diff
        diff.numberOfDiffs == 2
        diff.diffs.size() == 2
        diff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION].contains(it.name) }
        diff.diffs.each { ![DiffBuilder.DATA_CLASSES].contains(it.name) }
    }

    void 'diff dataModels with same modified data class - should show differences'() {
        given:
        //modified comparison key = label
        (DataClass) POST("$DATAMODELS_PATH/$leftDataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$rightDataModelId$DATACLASSES_PATH",
                [label: 'Test data class', description: 'other test description', minMultiplicity: -1], DataClass)

        //child data class
        DataClass childDataClass = (DataClass) POST("$DATAMODELS_PATH/$rightDataModelId$DATACLASSES_PATH/$rightDataClass.id$DATACLASSES_PATH",
                [label: 'Test child', description: 'child test description', minMultiplicity: -2], DataClass)

        and:
        //Diff endpoint does not return nested ObjectDiffs using httpClient.
        // Get with content fetches all data classes and nested objects in DM
        DataModel left = (DataModel) GET("$DATAMODELS_PATH/allContent/$leftDataModelId", DataModel)
        DataModel right = (DataModel) GET("$DATAMODELS_PATH/allContent/$rightDataModelId", DataModel)
        when:
        ObjectDiff diff = left.diff(right)
        then:
        diff

        diff.label == left.label
        diff.diffs.size() == 3
        diff.getNumberOfDiffs() == 5
        diff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> dataClasses = diff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()

        dataClasses.modified.numberOfDiffs.first() == 3
        dataClasses.modified.diffs.each { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified.diffs.last().last()

        child
        child.deleted.isEmpty()
        child.modified.isEmpty()

        BaseCollectionDiff childDiff = child.created.first()
        childDiff.id== childDataClass.id
        childDiff.label == childDataClass.label
    }

    void 'diff dataModels with dataType diffs'() {
        given:
        //modified comparison key = label
        DataType leftDataType = (DataType) POST("$DATAMODELS_PATH/$leftDataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)

        and:
        //Diff endpoint does not return nested ObjectDiffs using httpClient.
        // Get with content fetches all data classes and nested objects in DM
        DataModel left = (DataModel) GET("$DATAMODELS_PATH/allContent/$leftDataModelId", DataModel)
        DataModel right = (DataModel) GET("$DATAMODELS_PATH/allContent/$rightDataModelId", DataModel)
        when:
        ObjectDiff diff = left.diff(right)

        then:
        diff
        diff.numberOfDiffs == 3
        diff.diffs.size() == 3
        ArrayDiff<Collection> dataTypesDiff = diff.diffs.find {it.name == DiffBuilder.DATA_TYPE}
        dataTypesDiff.created.isEmpty()
        dataTypesDiff.modified.isEmpty()
        dataTypesDiff.deleted.size() ==1
        BaseCollectionDiff baseCollectionDiff =  dataTypesDiff.deleted.first()
        baseCollectionDiff.id == leftDataType.id
        baseCollectionDiff.label == leftDataType.label
    }

    //todo: dataelements not fetched in test. (Postman ok)
    void 'diff dataModels with DataClasses and DataElements'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = (DataClass) POST("$DATAMODELS_PATH/$leftDataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)
        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$rightDataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataType leftDataTypeResponse = (DataType)  POST("$DATAMODELS_PATH/$leftDataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)
        DataType rightDataTypeResponse = (DataType)  POST("$DATAMODELS_PATH/$rightDataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)


       DataElement leftDataElement = (DataElement)  POST("$DATAMODELS_PATH/$leftDataModelId$DATACLASSES_PATH/$leftDataClass.id$DATA_ELEMENTS_PATH",
               [ label: 'data element', description:  'The first data element description', dataType: [id: leftDataTypeResponse.id]], DataElement)

        and:
        //Diff endpoint does not return nested ObjectDiffs using httpClient.
        // Get with content fetches all data classes and nested objects in DM
        def dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$leftDataModelId/dataClasses/$leftDataClass.id/dataElements")
        dataElementListResponse.count == 1

        when:
        def retrievedDataElement = dataElementListResponse.getItems().first()
        //for readDataModel withContent:
        then:
        retrievedDataElement.dataType.id.toString() == leftDataTypeResponse.id.toString()

        DataModel left = (DataModel) GET("$DATAMODELS_PATH/allContent/$leftDataModelId", DataModel)
        DataModel right = (DataModel) GET("$DATAMODELS_PATH/allContent/$rightDataModelId", DataModel)
        when:
        ObjectDiff diff = left.diff(right)

        //todo: dataElements -investigate why not being fetched by dataModel
        then:
        diff
        ArrayDiff<Collection> dataElementsDiff = diff.diffs.find {it.name == DiffBuilder.DATA_ELEMENT}
      //  dataElementsDiff
    }
}
