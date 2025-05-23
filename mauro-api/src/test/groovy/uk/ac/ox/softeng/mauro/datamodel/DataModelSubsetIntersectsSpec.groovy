package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.datamodel.IntersectsData
import uk.ac.ox.softeng.mauro.domain.datamodel.IntersectsManyData
import uk.ac.ox.softeng.mauro.domain.datamodel.SubsetData
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
class DataModelSubsetIntersectsSpec  extends CommonDataSpec {

    @Inject
    @Shared
    FolderCacheableRepository folderCacheableRepository

    @Inject
    @Shared
    DataModelContentRepository dataModelContentRepository

    @Inject
    DataModelApi dataModelApi

    @Shared
    UUID dataModelId

    @Shared
    UUID numericDataElementId

    @Shared
    UUID stringDataElementId

    @Shared
    UUID booleanDataElementId

    @Shared
    UUID enumerationDataElementId

    @Shared
    UUID folderId

    @Shared
    UUID targetDataModelId

    void setupSpec() {
        Folder testFolder = folderCacheableRepository.save(folder())
        folderId = testFolder.id

        DataModel catalogueDataModel = DataModel.build {
            label 'Test data catalogue model'
            folder testFolder
            primitiveType {
                label "String"
            }
            primitiveType {
                label "Numeric"
            }
            enumerationType {
                label "Boolean"
                enumerationValue {
                    key "T"
                    value "True"
                }
                enumerationValue {
                    key "F"
                    value "False"
                }
            }
            dataClass {
                label "First class"
            }
            dataClass {
                label "Second class"
                dataElement {
                    label "My first Data Element"
                    dataType "Boolean"
                }
            }
            dataClass {
                label "Third class"
                dataElement {
                    label "Test Data Element 2"
                    dataType "Numeric"
                }
                dataElement {
                    label "Test Data Element 3"
                    dataType "Boolean"
                }
                dataClass {
                    label "Child class"
                    dataElement {
                        label "Test Data Element 4"
                        dataType "String"
                    }
                }
            }
        }

        DataType enumerationType = new DataType(
            label: 'Large EnumerationType',
            dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE,
            enumerationValues: (1..1000).collect {new EnumerationValue(key: "test_ev_$it", value: "Test EV $it")}
        )

        catalogueDataModel.dataTypes << enumerationType

        catalogueDataModel.dataClasses.find {it.label == 'Third class'}.dataClasses.find {it.label == 'Child class'}.dataElements << new DataElement(
            label: 'Test Data Element 5',
            dataType: enumerationType
        )

        catalogueDataModel.setAssociations()
        catalogueDataModel = dataModelContentRepository.saveWithContent(catalogueDataModel)

        dataModelId = catalogueDataModel.id
        stringDataElementId = catalogueDataModel.dataElements.find {it.label == 'Test Data Element 4'}.id
        numericDataElementId = catalogueDataModel.dataElements.find {it.label == 'Test Data Element 2'}.id
        booleanDataElementId = catalogueDataModel.dataElements.find {it.label == 'My first Data Element'}.id
        enumerationDataElementId = catalogueDataModel.dataElements.find {it.label == 'Test Data Element 5'}.id
    }

    void 'create new datamodel from subset'() {
        when: 'subset is created using empty datamodel'
        DataModel target = dataModelApi.create(folderId, new DataModel(label: 'Subset Target DataModel'))
        targetDataModelId = target.id
        DataModel response = dataModelApi.subset(dataModelId, targetDataModelId, new SubsetData(additions: [numericDataElementId, enumerationDataElementId]))

        then: 'subset endpoint succeeds'
        response.id == targetDataModelId

        when: 'subset elements are queried'
        DataModel targetUpdated = dataModelContentRepository.findWithContentById(targetDataModelId)

        UUID copiedNumericDataElementId = targetUpdated.dataElements.find {it.label == 'Test Data Element 2'}.id
        UUID copiedEnumerationDataElementId = targetUpdated.dataElements.find {it.label == 'Test Data Element 5'}.id
        UUID copiedThirdDataClassId = targetUpdated.allDataClasses.find {it.label == 'Third class'}.id
        UUID copiedChildDataClassId = targetUpdated.allDataClasses.find {it.label == 'Child class'}.id

        DataElement copiedNumericDataElement = dataElementApi.show(targetUpdated.id, copiedThirdDataClassId, copiedNumericDataElementId)
        DataElement copiedEnumerationDataElement = dataElementApi.show(targetUpdated.id, copiedChildDataClassId, copiedEnumerationDataElementId)

        then: 'subset elements have correct paths and types'
        copiedNumericDataElement
        copiedNumericDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Third class|de:Test Data Element 2'
        copiedNumericDataElement.dataType.label == 'Numeric'
        copiedEnumerationDataElement
        copiedEnumerationDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Third class|dc:Child class|de:Test Data Element 5'
        copiedEnumerationDataElement.dataType.label == 'Large EnumerationType'

        when: 'subset datatypes are queried'
        DataType copiedBooleanDataType = dataTypeApi.show(targetUpdated.id, copiedNumericDataElement.dataType.id)
        DataType copiedEnumerationDataType = dataTypeApi.show(targetUpdated.id, copiedEnumerationDataElement.dataType.id)

        then: 'subset datatypes have correct paths and types'
        copiedBooleanDataType.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE
        copiedBooleanDataType.label == 'Numeric'
        copiedBooleanDataType.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dt:Numeric'

        copiedEnumerationDataType.dataTypeKind == DataType.DataTypeKind.ENUMERATION_TYPE
        copiedEnumerationDataType.label == 'Large EnumerationType'
        copiedEnumerationDataType.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dt:Large EnumerationType'

        when: 'subset enumerationvalues are queried'
        ListResponse<EnumerationValue> enumerationValuesResponse = enumerationValueApi.list(targetUpdated.id, copiedEnumerationDataType.id)

        then: 'enumerationvalues are correct'
        enumerationValuesResponse
        enumerationValuesResponse.count == 1000
        enumerationValuesResponse.items.key.toSet() == (1..1000).collect {"test_ev_$it".toString()}.toSet()
    }

    void 'get intersectsMany of original and subset model'() {
        when:
        IntersectsManyData request = new IntersectsManyData(
            targetDataModelIds: [targetDataModelId],
            dataElementIds: [stringDataElementId, numericDataElementId, booleanDataElementId, enumerationDataElementId]
        )
        ListResponse<IntersectsData> response = dataModelApi.intersectsMany(dataModelId, request)

        then:
        response
        response.count == 1
        response.items.first().intersects.toSet() == [numericDataElementId, enumerationDataElementId].toSet()
    }

    void 'add more elements to subset model with subset'() {
        when:
        DataModel response = dataModelApi.subset(dataModelId, targetDataModelId, new SubsetData(additions: [stringDataElementId, numericDataElementId, booleanDataElementId]))

        then:
        response.id == targetDataModelId

        when:
        DataModel targetUpdated = dataModelContentRepository.findWithContentById(targetDataModelId)

        then:
        targetUpdated.dataElements.size() == 4
        targetUpdated.allDataClasses.size() == 3

        when: 'get each dataelement in subset datamodel'
        UUID copiedNumericDataElementId = targetUpdated.dataElements.find {it.label == 'Test Data Element 2'}.id
        UUID copiedEnumerationDataElementId = targetUpdated.dataElements.find {it.label == 'Test Data Element 5'}.id
        UUID copiedStringDataElementId = targetUpdated.dataElements.find {it.label == 'Test Data Element 4'}.id
        UUID copiedBooleanDataElementId = targetUpdated.dataElements.find {it.label == 'My first Data Element'}.id
        UUID copiedSecondDataClassId = targetUpdated.allDataClasses.find {it.label == 'Second class'}.id
        UUID copiedThirdDataClassId = targetUpdated.allDataClasses.find {it.label == 'Third class'}.id
        UUID copiedChildDataClassId = targetUpdated.allDataClasses.find {it.label == 'Child class'}.id
        DataElement copiedNumericDataElement = dataElementApi.show(targetUpdated.id, copiedThirdDataClassId, copiedNumericDataElementId)
        DataElement copiedEnumerationDataElement = dataElementApi.show(targetUpdated.id, copiedChildDataClassId, copiedEnumerationDataElementId)
        DataElement copiedStringDataElement = dataElementApi.show(targetUpdated.id, copiedChildDataClassId, copiedStringDataElementId)
        DataElement copiedBooleanDataElement = dataElementApi.show(targetUpdated.id, copiedSecondDataClassId, copiedBooleanDataElementId)

        then: 'subset dataelements have correct path'
        copiedNumericDataElement
        copiedNumericDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Third class|de:Test Data Element 2'
        copiedNumericDataElement.dataType.label == 'Numeric'
        copiedEnumerationDataElement
        copiedEnumerationDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Third class|dc:Child class|de:Test Data Element 5'
        copiedEnumerationDataElement.dataType.label == 'Large EnumerationType'
        copiedStringDataElement
        copiedStringDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Third class|dc:Child class|de:Test Data Element 4'
        copiedStringDataElement.dataType.label == 'String'
        copiedBooleanDataElement
        copiedBooleanDataElement.path.toString() == 'fo:Test folder|dm:Subset Target DataModel$main|dc:Second class|de:My first Data Element'
        copiedBooleanDataElement.dataType.label == 'Boolean'
    }
}
