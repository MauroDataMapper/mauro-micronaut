package org.maurodata.test.domain.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.domain.model.Item
import org.maurodata.domain.terminology.Terminology

import spock.lang.Specification

/**
 * DataModelSpec is a class for testing functionality relating to the DataModel class
 * @see DataModel
 */
class DataModelSpec extends Specification {

    static UUID dataClassReferenceId=UUID.randomUUID()

    static DataModel testDataModel = DataModel.build(label: 'My Test DataModel') {
        author 'James Welch'
        description 'This is an example of a data model corresponding to a scrape of a made-up database'
        id UUID.randomUUID()
        primitiveType {
            label 'string'
            description 'character string'
            id UUID.randomUUID()
        }
        primitiveType {
            label 'integer'
            description 'a whole number, may be positive or negative, with no maximum or minimum'
            id UUID.randomUUID()
        }
        primitiveType {
            label 'height'
            units 'centimeters'
            description 'a whole number, may be positive or negative, with no maximum or minimum'
            id UUID.randomUUID()
        }
        enumerationType {
            label 'Yes/No'
            description 'Either a yes, or a no'
            enumerationValue {
                key 'Y'
                value 'Yes'
                id UUID.randomUUID()
            }
            enumerationValue(key: 'N', value: 'No', id: UUID.randomUUID())
            id UUID.randomUUID()
        }

        dataClass {
            label 'My First DataClass'
            description 'Here is the description of the first DataClass'
            id UUID.randomUUID()
            metadata("com.test2", "key3", "value3")
            dataElement {
                label 'A data element that uses dataType.referenceClass'
                description 'Something about the data element here...'
                id UUID.randomUUID()
            }
        }

        dataClass {
            label 'My Second DataClass'
            description 'Here is the description of the second DataClass'
            id dataClassReferenceId
            dataClass {
                label 'My Third DataClass'
                description 'Here is the description of the third DataClass'
                id UUID.randomUUID()
                extendsDataClass 'My First DataClass'
                dataElement {
                    label 'A data element'
                    description 'Something about the data element here...'
                    dataType 'Yes/No'
                    id UUID.randomUUID()
                    metadata("com.test1", "key1", "value1")
                    summaryMetadata(label:'test label', description: 'test description', summaryMetadataType: SummaryMetadataType.STRING)
                }
                dataElement {
                    label 'Another data element'
                    description 'Something about the data element here...'
                    id UUID.randomUUID()
                    primitiveType {
                        label 'date'
                        description 'A date'
                        id UUID.randomUUID()
                    }
                    metadata("com.test1", "key2", "value2")
                }
            }
        }
    }

    static {
        DataClass dataClass1 = testDataModel.dataClasses.find {
            it.label == 'My First DataClass'
        }
        DataClass dataClass2 = testDataModel.dataClasses.find {
            it.label == 'My Second DataClass'
        }

        DataClass dataClass2Stub = new DataClass()
        dataClass2Stub.id = dataClass2.id

        DataType classReferencing = new DataType()
        classReferencing.id = UUID.randomUUID()
        classReferencing.domainType = DataType.DataTypeKind.REFERENCE_TYPE
        classReferencing.referenceClass = dataClass2Stub
        dataClass1.dataElements.get(0).dataType = classReferencing

        testDataModel.dataTypes.add(classReferencing)
        classReferencing.dataModel = testDataModel
    }

    def "Test the DSL for creating objects"() {

        when:
        testDataModel

        then:
        testDataModel.dataTypes.size() == 6

        testDataModel.dataTypes.findAll {
            it.domainType == 'PrimitiveType'
        }.size() == 4

        testDataModel.dataTypes.findAll {
            it.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE
        }.size() == 4

        testDataModel.dataTypes.findAll {
            it.domainType == 'EnumerationType'
        }.size() == 1

        testDataModel.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.size() == 2

        testDataModel.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.size() == 2

        testDataModel.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.key.sort() == ['N', 'Y']

        testDataModel.dataClasses.size() == 2
        testDataModel.allDataClasses.size() == 3

        testDataModel.dataClasses.find {
            it.label == 'My First DataClass'
        }

        DataClass dataClass1 = testDataModel.dataClasses.find {
            it.label == 'My First DataClass'
        }

        DataClass dataClass2 = testDataModel.dataClasses.find {
            it.label == 'My Second DataClass'
        }
        dataClass2.dataClasses.size() == 1
        DataClass dataClass3 = dataClass2.dataClasses.find {
            it.label == 'My Third DataClass'
        }

        testDataModel.getChildDataClasses().size() == 2

        dataClass3.dataElements.size() == 2
        dataClass3.dataElements.every {
            it.dataType
        }
        dataClass3.extendsDataClasses.size() == 1
        dataClass3.extendsDataClasses.first() == dataClass1
    }

    def 'clone the datamodel -should deep copy object and all modelitems'() {
        given:
        DataModel original = testDataModel
        when:
        DataModel cloned = original.clone()

        then:
        !cloned.is(original)

        cloned.dataTypes == original.dataTypes
        !cloned.dataTypes.is(original.dataTypes)  //groovy object equality. cloned is not original

        cloned.allDataClasses == original.allDataClasses
        !cloned.allDataClasses.is(original.allDataClasses)

        cloned.dataElements == original.dataElements
        !cloned.dataElements.is(original.dataElements)

        cloned.enumerationValues == original.enumerationValues
        !cloned.enumerationValues.is(original.enumerationValues)  //groovy object equal

        cloned.allDataClasses.containsAll(cloned.dataElements.dataClass)
        original.allDataClasses.containsAll(original.dataElements.dataClass)

        DataClass clonedDataClass1 = cloned.allDataClasses.find {it.label == 'My First DataClass'}
        DataClass clonedDataClass3 = cloned.allDataClasses.find {it.label == "My Third DataClass"}
        clonedDataClass3.extendsDataClasses.size() == 1
        clonedDataClass3.extendsDataClasses.first().label == 'My First DataClass'
        clonedDataClass3.extendsDataClasses.first().is(clonedDataClass1)
        clonedDataClass1.extendedBy.size() == 1
        clonedDataClass1.extendedBy.first().is(clonedDataClass3)

        cloned.dataElements.dataType.hashCode() != original.dataElements.dataType.hashCode()
        cloned.dataElements.dataType.id == original.dataElements.dataType.id
        cloned.dataTypes.containsAll(cloned.dataElements.dataType)
        original.dataTypes.containsAll(original.dataElements.dataType)

        !cloned.dataElements.dataType.is(original.dataElements.dataType)

        ObjectDiff objectDiff = original.diff(cloned)
        objectDiff.numberOfDiffs == 0
    }

    def 'deep clone the datamodel -should deep copy object and all modelitems and facets'() {
        given:
        DataModel original = testDataModel
        when:
        DataModel cloned = (DataModel) original.deepClone()
        cloned.setAssociations()
        then:
        !cloned.is(original)

        cloned.dataTypes == original.dataTypes
        !cloned.dataTypes.is(original.dataTypes)  //groovy object equality. cloned is not original

        cloned.allDataClasses == original.allDataClasses
        !cloned.allDataClasses.is(original.allDataClasses)

        cloned.dataElements == original.dataElements
        !cloned.dataElements.is(original.dataElements)

        cloned.enumerationValues == original.enumerationValues
        !cloned.enumerationValues.is(original.enumerationValues)  //groovy object equal

        cloned.allDataClasses.containsAll(cloned.dataElements.dataClass)
        original.allDataClasses.containsAll(original.dataElements.dataClass)

        DataClass originalDataClass1 = original.allDataClasses.find {it.label == 'My First DataClass'}
        originalDataClass1.extendedBy.size() == 1
        originalDataClass1.metadata.find { it.namespace == 'com.test2' && it.key == 'key3' && it.value == 'value3'}

        DataClass clonedDataClass1 = cloned.allDataClasses.find {it.label == 'My First DataClass'}
        clonedDataClass1.extendedBy.size() == 1
        clonedDataClass1.metadata.find { it.namespace == 'com.test2' && it.key == 'key3' && it.value == 'value3'}

        DataClass originalDataClass3 = original.allDataClasses.find {it.label == "My Third DataClass"}
        DataElement originalDataElement = originalDataClass3.dataElements.find {it.label == 'A data element' }
        originalDataElement.metadata.find {it.namespace == 'com.test1' && it.key == 'key1' && it.value == 'value1' }
        originalDataElement.summaryMetadata.find { it.label == 'test label' && it.description == 'test description' && it.summaryMetadataType == SummaryMetadataType.STRING }

        DataClass clonedDataClass3 = cloned.allDataClasses.find {it.label == "My Third DataClass"}
        clonedDataClass3.extendsDataClasses.size() == 1
        clonedDataClass3.extendsDataClasses.first().label == 'My First DataClass'
        clonedDataClass3.extendsDataClasses.first().is(clonedDataClass1)
        clonedDataClass1.extendedBy.size() == 1
        clonedDataClass1.extendedBy.first().is(clonedDataClass3)

        DataElement clonedDataElement = clonedDataClass3.dataElements.find {it.label == 'A data element' }
        clonedDataElement.metadata.find {it.namespace == 'com.test1' && it.key == 'key1' && it.value == 'value1' }
        clonedDataElement.summaryMetadata.find { it.label == 'test label' && it.description == 'test description' && it.summaryMetadataType == SummaryMetadataType.STRING }

        cloned.dataElements.dataType.hashCode() != original.dataElements.dataType.hashCode()
        cloned.dataElements.dataType.id == original.dataElements.dataType.id
        cloned.dataTypes.containsAll(cloned.dataElements.dataType)
        original.dataTypes.containsAll(original.dataElements.dataType)

        !cloned.dataElements.dataType.is(original.dataElements.dataType)

        ObjectDiff objectDiff = original.diff(cloned)
        objectDiff.numberOfDiffs == 0
    }

    def 'itemLookupById should give two Classes'() {
        given:
        testDataModel
        when:
        Map<UUID, Set<Item>> itemsById = testDataModel.itemLookupById()
        then:
        Set<Item> itemsOfInterest = itemsById.get(dataClassReferenceId)
        itemsOfInterest.size() == 2

        List<Item> itemsOfInterestList = itemsOfInterest.toList()

        DataClass dataClass1=((DataClass) itemsOfInterestList.get(0)).deepClone()
        DataClass dataClass2=((DataClass) itemsOfInterestList.get(1)).deepClone()
        dataClass2.copyInto(dataClass1)

        System.out.println(dataClass1.dump())

        dataClass1.label
    }
}