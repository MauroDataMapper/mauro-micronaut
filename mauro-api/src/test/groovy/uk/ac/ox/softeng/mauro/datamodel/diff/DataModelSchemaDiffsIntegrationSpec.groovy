package uk.ac.ox.softeng.mauro.datamodel.diff


import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelSchemaDiffsIntegrationSpec extends CommonDataSpec {
    static String NAME = 'name'
    static String CREATED = 'created'
    static String DELETED = 'deleted'

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    DataModel left
    @Shared
    DataModel right

    void setup() {
        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        this.left = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        this.right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH",
                [label: 'Test data model', description: 'right description', author: ' right test author'], DataModel)

    }

    void 'diff same datamodel -should have no differences'() {
        when:
        ObjectDiff diff = (ObjectDiff) GET("$DATAMODELS_PATH/$left.id/diff/$left.id", ObjectDiff)
        then:
        diff
        diff.label == left.label
        diff.leftId == left.id.toString()
        diff.rightId == left.id.toString()
        diff.numberOfDiffs == 0
    }

    void 'diff dataModels with same data class - should have no differences'() {
        given:
        //modified comparison key = label
        (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        when:
        ObjectDiff diff = (ObjectDiff) GET("$DATAMODELS_PATH/$left.id/diff/$right.id", ObjectDiff)

        then:
        diff
        diff.numberOfDiffs == 2
        diff.diffs.size() == 2
        diff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION].contains(it.name) }
        diff.diffs.each { ![DiffBuilder.DATA_CLASSES].contains(it.name) }
    }

    void 'diff dataModels with same modified data class - should show diff with nested child data class '() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH",
                [label: 'Test data class', description: 'other test description', minMultiplicity: -1], DataClass)

        //child data class
        DataClass childDataClass = (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH/$rightDataClass.id$DATACLASSES_PATH",
                [label: 'Test child', description: 'child test description', minMultiplicity: -2], DataClass)

        when:
        Map<String, Object> diff = GET("$DATAMODELS_PATH/$left.id/diff/$right.id", Map<String, Object>)

        then:
        diff
        diff.count == 5
        ArrayDiff<Collection> dataClasses = diff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].count == 3
        dataClasses.modified[0].diffs.each { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified[0].diffs.find { it.name == DiffBuilder.DATA_CLASSES }
        child
        child.deleted.isEmpty()
        child.modified.isEmpty()
        child.created.size() == 1
        child.created[0].get(DiffBuilder.ID_KEY) == childDataClass.id.toString()
        child.created[0].get(DiffBuilder.LABEL) == childDataClass.label
    }


    void 'diff dataModels with same modified data class with left and right sides reversed - count should be same as previous test '() {
        given:
        //modified comparison key = label
        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataClass leftDataClass = (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH",
                [label: 'Test data class', description: 'other test description', minMultiplicity: -1], DataClass)

        //child data class now on left
        DataClass childDataClass = (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH/$leftDataClass.id$DATACLASSES_PATH",
                [label: 'Test child', description: 'child test description', minMultiplicity: -2], DataClass)

        when:
        Map<String, Object> diff = GET("$DATAMODELS_PATH/$left.id/diff/$right.id", Map<String, Object>)

        then:
        diff
        diff.count == 5
        ArrayDiff<Collection> dataClasses = diff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].count == 3
        dataClasses.modified[0].diffs.each { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified[0].diffs.find { it.name == DiffBuilder.DATA_CLASSES }
        child
        child.created.isEmpty()
        child.modified.isEmpty()
        child.deleted.size() == 1
        child.deleted[0].get(DiffBuilder.ID_KEY) == childDataClass.id.toString()
        child.deleted[0].get(DiffBuilder.LABEL) == childDataClass.label
    }


    void 'diff dataModels with dataType diffs'() {
        given:
        //modified comparison key = label
        DataType leftDataType = (DataType) POST("$DATAMODELS_PATH/$left.id$DATATYPES_PATH", dataTypesPayload(), DataType)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id/diff/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.count == 3
        ArrayDiff<Collection> dataTypesDiff = diffMap.diffs.find { it.name == DiffBuilder.DATA_TYPE }
        dataTypesDiff.created.isEmpty()
        dataTypesDiff.modified.isEmpty()
        dataTypesDiff.deleted.size() == 1

        dataTypesDiff.deleted.size() == 1
        dataTypesDiff.deleted[0].get(DiffBuilder.ID_KEY) == leftDataType.id.toString()
        dataTypesDiff.deleted[0].get(DiffBuilder.LABEL) == leftDataType.label
    }


    void 'diff dataModels with DataClasses and DataElements -the dataClass should show nested dataElement'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH", dataClassPayload(), DataClass)
        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataType leftDataTypeResponse = (DataType) POST("$DATAMODELS_PATH/$left.id$DATATYPES_PATH", dataTypesPayload(), DataType)

        DataElement leftDataElement = (DataElement) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH/$leftDataClass.id$DATA_ELEMENTS_PATH",
                [label: 'data element', description: 'The first data element description', dataType: [id: leftDataTypeResponse.id]], DataElement)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id/diff/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> dataClassesDiff = diffMap.diffs.find { it.name == DiffBuilder.DATA_CLASSES }
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.modified.size() == 1
        dataClassesDiff.modified.diffs.size() == 1
        dataClassesDiff.modified.diffs.each {
            [NAME, DELETED].contains(it.name)
        }

        def nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        Map<String, Object> dataElementMap = nestedDataElement[0] as Map<String, Object>
        dataElementMap.keySet().flatten().sort().containsAll([DELETED, NAME])
        def dataElement = dataElementMap.get(DELETED)
        dataElement[0].get(DiffBuilder.LABEL) == leftDataElement.label
        dataElement[0].get(DiffBuilder.ID_KEY) == leftDataElement.id.toString()
    }

    void 'diff dataModels with DataClasses and nested DataElements on RHS  -should give same counts as when on LHS'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = (DataClass) POST("$DATAMODELS_PATH/$left.id$DATACLASSES_PATH", dataClassPayload(), DataClass)
        DataClass rightDataClass = (DataClass) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH", dataClassPayload(), DataClass)

        DataType leftDataTypeResponse = (DataType) POST("$DATAMODELS_PATH/$left.id$DATATYPES_PATH", dataTypesPayload(), DataType)
        DataType rightDataTypeResponse = (DataType) POST("$DATAMODELS_PATH/$right.id$DATATYPES_PATH", dataTypesPayload(), DataType)


        DataElement rightDataElement = (DataElement) POST("$DATAMODELS_PATH/$right.id$DATACLASSES_PATH/$rightDataClass.id$DATA_ELEMENTS_PATH",
                [label: 'data element', description: 'The first data element description', dataType: [id: rightDataTypeResponse.id]], DataElement)

        //validation of dataElement on dataClass
        and:
        def dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$right.id/dataClasses/$rightDataClass.id/dataElements")
        dataElementListResponse.count == 1

        when:
        def retrievedDataElement = dataElementListResponse.getItems().first()
        then:
        retrievedDataElement.dataType.id.toString() == rightDataTypeResponse.id.toString()

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id/diff/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> dataClassesDiff = diffMap.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.modified.size() == 1

        def nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        Map<String, Object> dataElementMap = nestedDataElement[0] as Map<String, Object>
        dataElementMap.keySet().flatten().sort().containsAll([CREATED, NAME])
        def dataElement = dataElementMap.get(CREATED)
        dataElement[0].get(DiffBuilder.LABEL) == rightDataElement.label
        dataElement[0].get(DiffBuilder.ID_KEY) == rightDataElement.id.toString()
    }
}


