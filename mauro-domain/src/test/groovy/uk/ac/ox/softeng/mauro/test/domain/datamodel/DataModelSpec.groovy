package uk.ac.ox.softeng.mauro.test.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import spock.lang.Specification

/**
 * DataModelSpec is a class for testing functionality relating to the DataModel class
 * @see DataModel
 */
class DataModelSpec extends Specification {

    static DataModel testDataModel = DataModel.build(label: 'My Test DataModel') {
        author 'James Welch'
        description 'This is an example of a data model corresponding to a scrape of a made-up database'
        id UUID.randomUUID()
        primitiveType {
            label 'string'
            description 'character string'
        }
        primitiveType {
            label 'integer'
            description 'a whole number, may be positive or negative, with no maximum or minimum'
        }
        primitiveType {
            label 'height'
            units 'centimeters'
            description 'a whole number, may be positive or negative, with no maximum or minimum'
        }
        enumerationType {
            label 'Yes/No'
            description 'Either a yes, or a no'
            enumerationValue {
                key 'Y'
                value 'Yes'
            }
            enumerationValue(key: 'N', value: 'No')
        }

        dataClass {
            label 'My First DataClass'
            description 'Here is the description of the first DataClass'
            id UUID.randomUUID()
        }

        dataClass {
            label 'My Second DataClass'
            description 'Here is the description of the second DataClass'
            id UUID.randomUUID()
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
                }
                dataElement {
                    label 'Another data element'
                    description 'Something about the data element here...'
                    id UUID.randomUUID()
                    primitiveType {
                        label 'date'
                        description 'A date'
                    }
                }
            }
        }

    }


    def "Test the DSL for creating objects"() {

        when:
        testDataModel

        then:
        testDataModel.dataTypes.size() == 5

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

        cloned.dataElements.dataType == original.dataElements.dataType
        cloned.dataTypes.containsAll(cloned.dataElements.dataType)
        original.dataTypes.containsAll(original.dataElements.dataType)

        !cloned.dataElements.dataType.is(original.dataElements.dataType)

        ObjectDiff objectDiff = original.diff(cloned)
        objectDiff.numberOfDiffs == 0
    }

}