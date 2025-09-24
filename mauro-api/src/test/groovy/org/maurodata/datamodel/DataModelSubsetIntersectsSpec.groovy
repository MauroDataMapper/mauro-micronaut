package org.maurodata.datamodel

import org.maurodata.api.datamodel.DataModelApi
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.datamodel.IntersectsData
import org.maurodata.domain.datamodel.IntersectsManyData
import org.maurodata.domain.datamodel.SubsetData
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.web.PaginationParams
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

        when: 'subset enumerationvalues are queried with pager'
        ListResponse<EnumerationValue> enumerationValuesResponsePaged = enumerationValueApi.list(targetUpdated.id, copiedEnumerationDataType.id, new PaginationParams(max: 10))

        then: 'paged enumerationvalues are correct and sorted by label ascending'
        enumerationValuesResponsePaged
        enumerationValuesResponsePaged.count == 1000
        enumerationValuesResponsePaged.items.size() == 10
        enumerationValuesResponsePaged.items.key.containsAll(["test_ev_1", "test_ev_10", "test_ev_100", "test_ev_1000", "test_ev_101", "test_ev_102", "test_ev_103", "test_ev_104", "test_ev_105", "test_ev_106"])
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

    void 'delete elements from model with subset'() {
        when:
        DataModel response = dataModelApi.subset(dataModelId, targetDataModelId, new SubsetData(deletions: [stringDataElementId, enumerationDataElementId]))

        then:
        response.id == targetDataModelId

        when:
        DataModel targetUpdated = dataModelContentRepository.findWithContentById(targetDataModelId)

        then:
        targetUpdated.dataElements.size() == 2
        targetUpdated.allDataClasses.size() == 3

        and: 'get each dataelement in subset datamodel'
        targetUpdated.dataElements.find {it.label == 'Test Data Element 2'}.id
        targetUpdated.dataElements.find {it.label == 'My first Data Element'}.id
        targetUpdated.allDataClasses.find {it.label == 'Second class'}.id
        targetUpdated.allDataClasses.find {it.label == 'Third class'}.id
        targetUpdated.allDataClasses.find {it.label == 'Child class'}.id
    }
}
