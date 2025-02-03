package uk.ac.ox.softeng.mauro.datamodel.diff

import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.api.facet.AnnotationApi
import uk.ac.ox.softeng.mauro.api.facet.MetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.domain.diff.FieldDiff

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

    @Shared
    UUID folderId

    @Shared
    DataModel left
    @Shared
    DataModel right

    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject DataClassApi dataClassApi
    @Inject DataTypeApi dataTypeApi
    @Inject DataElementApi dataElementApi
    @Inject MetadataApi metadataApi
    @Inject AnnotationApi annotationApi
    @Inject SummaryMetadataApi summaryMetadataApi
    @Inject SummaryMetadataReportApi summaryMetadataReportApi

    void setup() {
        Folder response = folderApi.create(folder())
        folderId = response.id

        this.left = dataModelApi.create(folderId, dataModelPayload())

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
        objectDiff.numberOfDiffs == 4
        objectDiff.diffs.size() == 4
        objectDiff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL, PATH_IDENTIFIER].contains(it.name) }
        objectDiff.diffs.each { ![DiffBuilder.DATA_CLASSES].contains(it.name) }
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
        objectDiff.numberOfDiffs == 7
        ArrayDiff<Collection> dataClasses = objectDiff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].numberOfDiffs == 3
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
        DataClass rightDataClass = dataClassApi.create(right.id, dataClassPayload())

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
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 7
        ArrayDiff<Collection> dataClasses = objectDiff.diffs.find { it -> it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClasses.created.isEmpty()
        dataClasses.deleted.isEmpty()
        dataClasses.modified.size() == 1
        dataClasses.modified[0].numberOfDiffs == 3
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
        DataType leftDataType =
            dataTypeApi.create(left.id, dataTypesPayload())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.numberOfDiffs == 5
        ArrayDiff<Collection> dataTypesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_TYPE }
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
        ArrayDiff<Collection> dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES }
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.modified.size() == 1
        dataClassesDiff.modified.diffs.size() == 1
        dataClassesDiff.modified.diffs.each {
            [NAME, DELETED].contains(it.name)
        }

        Collection<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = nestedDataElement[0]
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
        ArrayDiff<Collection> dataClassesDiff = objectDiff.diffs.find { it.name == DiffBuilder.DATA_CLASSES } as ArrayDiff<Collection>
        dataClassesDiff.deleted.isEmpty()
        dataClassesDiff.created.isEmpty()
        dataClassesDiff.modified.size() == 1

        List<FieldDiff> nestedDataElement = dataClassesDiff.modified.diffs[0]
        nestedDataElement.size() == 1
        ArrayDiff dataElementMap = nestedDataElement[0]
        List<DataElement> dataElement = dataElementMap.created
        dataElement[0].label == rightDataElement.label
        dataElement[0].id == rightDataElement.id.toString()
    }
}


