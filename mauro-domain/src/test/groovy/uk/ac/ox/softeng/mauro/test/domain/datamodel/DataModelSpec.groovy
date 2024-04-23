package uk.ac.ox.softeng.mauro.test.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import spock.lang.Specification

/**
 * DataModelSpec is a class for testing functionality relating to the DataModel class
 * @see Terminology
 */
class DataModelSpec extends Specification {

    static DataModel testDataModel = DataModel.build (label: 'My Test DataModel') {
        author 'James Welch'
        description 'This is an example of a data model corresponding to a scrape of a made-up database'
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
        }

        dataClass {
            label 'My Second DataClass'
            description 'Here is the description of the second DataClass'

            dataClass {
                label 'My Third DataClass'
                description 'Here is the description of the third DataClass'
                extendsDataClass 'My First DataClass'
                dataElement {
                    label 'A data element'
                    description 'Something about the data element here...'
                    dataType 'Yes/No'
                }
                dataElement {
                    label 'Another data element'
                    description 'Something about the data element here...'
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

}