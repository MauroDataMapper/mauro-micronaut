package org.maurodata.datamodel.diff


import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.diff.ArrayDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelSchemaDiffsIntegrationSpec extends CommonDataSpec {
    static String NAME = 'name'
    // static String CREATED = 'created'
    static String DELETED = 'deleted'

    @Shared
    UUID folderId

    @Shared
    DataModel left
    @Shared
    DataModel left2
    @Shared
    DataModel right

    void setup() {
        Folder response = folderApi.create(folder())
        folderId = response.id

        this.left = dataModelApi.create(folderId, dataModelPayload())
        this.left2 = dataModelApi.create(folderId, dataModelPayload())

        this.right = dataModelApi.create(folderId, new DataModel(
            label: 'Test data model',
            description: 'right description',
            author: ' right test author'))
    }

    void 'diff same datamodel -should have no differences'() {
        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left.id)
        then:
        objectDiff
        objectDiff.label == left.label
        objectDiff.leftId == left.id.toString()
        objectDiff.rightId == left.id.toString()
        objectDiff.numberOfDiffs == 0
    }

    void 'diff dataModels with same data class - data class should have no differences'() {
        given:
        //modified comparison key = label
        dataClassApi.create(left.id, dataClassPayload())
        dataClassApi.create(right.id, dataClassPayload())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 3
        objectDiff.diffs.size() == 3
        objectDiff.diffs.every { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL].contains(it.name) }
        objectDiff.diffs.every { ![DiffBuilder.DATA_CLASSES].contains(it.name) }
    }

    void 'diff dataModels with same modified data class - should show diff with nested child data class '() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = dataClassApi.create(left.id, dataClassPayload())

        DataClass rightDataClass = dataClassApi.create(right.id, new DataClass(
            label: 'Test data class',
            description: 'other test description',
            minMultiplicity: -1))

        //child data class
        DataClass childDataClass = dataClassApi.create(right.id, rightDataClass.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 6
        ArrayDiff<Collection> dataClasses = objectDiff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].numberOfDiffs == 3
        dataClasses.modified[0].diffs.each { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified[0].diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        child
        child.deleted.isEmpty()
        child.modified.isEmpty()
        child.created.size() == 1
        child.created[0].get(DiffBuilder.ID_KEY) == childDataClass.id.toString()
        child.created[0].get(DiffBuilder.LABEL) == childDataClass.label
    }

    void 'diff dataModels with same modified data class 2 - should show diff with nested child data class '() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = dataClassApi.create(left.id, dataClassPayload())

        DataClass rightDataClass = dataClassApi.create(left2.id, new DataClass(
                label: 'Test data class',
                description: 'other test description',
                minMultiplicity: -1))

        //child data class
        DataClass childDataClass = dataClassApi.create(left2.id, rightDataClass.id, new DataClass(
                label: 'Test child',
                description: 'child test description',
                minMultiplicity: -2))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left2.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 3
        ArrayDiff<Collection> dataClasses = objectDiff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].numberOfDiffs == 3
        dataClasses.modified[0].diffs.every { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified[0].diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
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
        DataClass rightDataClass = dataClassApi.create(left2.id, dataClassPayload())

        DataClass leftDataClass = dataClassApi.create(left.id, new DataClass(
            label: 'Test data class',
            description: 'other test description',
            minMultiplicity: -1))

        //child data class now on left
        DataClass childDataClass = dataClassApi.create(left.id,leftDataClass.id, new DataClass(
            label: 'Test child',
            description: 'child test description',
            minMultiplicity: -2))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left2.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 3
        ArrayDiff<Collection> dataClasses = objectDiff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].numberOfDiffs == 3
        dataClasses.modified[0].diffs.every { [DiffBuilder.DESCRIPTION, DiffBuilder.MIN_MULTIPILICITY, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> child = dataClasses.modified[0].diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
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
        DataType leftDataType =
            dataTypeApi.create(left.id, dataTypesPayload())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 4
        ArrayDiff<Collection> dataTypesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_TYPE } as ArrayDiff<Collection>
        dataTypesDiff.created.isEmpty()
        dataTypesDiff.modified.isEmpty()
        dataTypesDiff.deleted.size() == 1

        dataTypesDiff.deleted.size() == 1
        dataTypesDiff.deleted[0].get(DiffBuilder.ID_KEY) == leftDataType.id.toString()
        dataTypesDiff.deleted[0].get(DiffBuilder.LABEL) == leftDataType.label
    }

    void 'diff dataModels with dataType diffs 2'() {
        given:
        //modified comparison key = label
        DataType leftDataType =
            dataTypeApi.create(left.id, dataTypesPayload())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left2.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 1
        ArrayDiff<Collection> dataTypesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_TYPE } as ArrayDiff<Collection>
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
        DataClass leftDataClass =
            dataClassApi.create(left.id, dataClassPayload())
        DataClass rightDataClass =
            dataClassApi.create(right.id, dataClassPayload())

        DataType leftDataTypeResponse =
            dataTypeApi.create(left.id, dataTypesPayload())

        DataElement leftDataElement = dataElementApi.create(left.id, leftDataClass.id, new DataElement(
            label: 'data element',
            description: 'The first data element description',
            dataType: leftDataTypeResponse))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff<Collection> dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.modified.size() == 1
        dataClassesDiff.modified.diffs.size() == 1
        dataClassesDiff.modified.diffs.each {
            [NAME, DELETED].contains(it.name)
        }

        Collection<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = (ArrayDiff) nestedDataElement[0]
        Collection<FieldDiff> dataElement = dataElementMap.deleted
        dataElement[0].get(DiffBuilder.LABEL) == leftDataElement.label
        dataElement[0].get(DiffBuilder.ID_KEY) == leftDataElement.id.toString()
    }

    void 'diff dataModels with DataClasses and DataElements -the dataClass should show nested dataElement 2'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass =
            dataClassApi.create(left.id, dataClassPayload())
        DataClass rightDataClass =
            dataClassApi.create(left2.id, dataClassPayload())

        DataType leftDataTypeResponse =
            dataTypeApi.create(left.id, dataTypesPayload())

        DataElement leftDataElement = dataElementApi.create(left.id, leftDataClass.id, new DataElement(
            label: 'data element',
            description: 'The first data element description',
            dataType: leftDataTypeResponse))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left2.id)

        then:
        objectDiff
        objectDiff.diffs.every { [DiffBuilder.DATA_CLASSES, DiffBuilder.DATA_ELEMENTS, DiffBuilder.DATA_TYPE].contains(it.name) }
        ArrayDiff<Collection> dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.modified.size() == 1
        dataClassesDiff.modified.diffs.size() == 1
        dataClassesDiff.modified.diffs.each {
            [NAME, DELETED].contains(it.name)
        }

        Collection<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = (ArrayDiff) nestedDataElement[0]
        Collection<FieldDiff> dataElement = dataElementMap.deleted
        dataElement[0].get(DiffBuilder.LABEL) == leftDataElement.label
        dataElement[0].get(DiffBuilder.ID_KEY) == leftDataElement.id.toString()
    }

    void 'diff dataModels with DataClasses and nested DataElements on RHS  -should give same counts as when on LHS'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = dataClassApi.create(left.id, dataClassPayload())
        DataClass rightDataClass = dataClassApi.create(right.id, dataClassPayload())

        DataType leftDataTypeResponse = dataTypeApi.create(left.id, dataTypesPayload())
        DataType rightDataTypeResponse = dataTypeApi.create(right.id, dataTypesPayload())


        DataElement rightDataElement = dataElementApi.create(right.id, rightDataClass.id,
                                                             new DataElement(label: 'data element',
                                                                             description: 'The first data element description',
                                                                             dataType: rightDataTypeResponse))

        //validation of dataElement on dataClass
        and:
        ListResponse<DataElement> dataElementListResponse =
            dataElementApi.list(right.id, rightDataClass.id)
        dataElementListResponse.count == 1

        when:
        DataElement retrievedDataElement = dataElementListResponse.getItems().first()
        then:
        retrievedDataElement.dataType.id.toString() == rightDataTypeResponse.id.toString()

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.DATA_CLASSES].contains(it.name) }
        ArrayDiff dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.modified.size() == 1

        List<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = (ArrayDiff) nestedDataElement[0]
        List<Map> dataElement = dataElementMap.created
        dataElement[0].label == rightDataElement.label
        dataElement[0].id == rightDataElement.id.toString()
    }

    void 'diff dataModels with DataClasses and nested DataElements on RHS  -should give same counts as when on LHS 2'() {
        given:
        //modified comparison key = label
        DataClass leftDataClass = dataClassApi.create(left.id, dataClassPayload())
        DataClass rightDataClass = dataClassApi.create(left2.id, dataClassPayload())

        DataType leftDataTypeResponse = dataTypeApi.create(left.id, dataTypesPayload())
        DataType rightDataTypeResponse = dataTypeApi.create(left2.id, dataTypesPayload())


        DataElement rightDataElement = dataElementApi.create(left2.id, rightDataClass.id,
                new DataElement(label: 'data element',
                                description: 'The first data element description',
                                dataType: rightDataTypeResponse))

        //validation of dataElement on dataClass
        and:
        ListResponse<DataElement> dataElementListResponse =
            dataElementApi.list(left2.id, rightDataClass.id)
        dataElementListResponse.count == 1

        when:
        DataElement retrievedDataElement = dataElementListResponse.getItems().first()
        then:
        retrievedDataElement.dataType.id.toString() == rightDataTypeResponse.id.toString()

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, left2.id)

        then:
        objectDiff
        objectDiff.diffs.every { [DiffBuilder.DATA_CLASSES, DiffBuilder.DATA_ELEMENTS, DiffBuilder.DATA_TYPE].contains(it.name) }
        ArrayDiff<Collection> dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.modified.size() == 1

        List<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = nestedDataElement[0] as ArrayDiff<Collection>
        List<Map> dataElement = dataElementMap.created as List<Map>
        dataElement[0].label == rightDataElement.label
        dataElement[0].id == rightDataElement.id.toString()
    }
}



