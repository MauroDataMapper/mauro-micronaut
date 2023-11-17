package uk.ac.ox.softeng.mauro.domain.test.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import spock.lang.Specification

/**
 * DataModelSpec is a class for testing functionality relating to the DataModel class
 * @see Terminology
 */
class DataModelSpec extends Specification {

    def "Test the DSL for creating objects"() {

        when:

        DataModel dataModel1 = DataModel.build (label: 'My Test DataModel') {
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

        }

        then:
        dataModel1.dataTypes.size() == 4

        dataModel1.dataTypes.findAll {
            it.domainType == 'PrimitiveType'
        }.size() == 3

        dataModel1.dataTypes.findAll {
            it.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE
        }.size() == 3

        dataModel1.dataTypes.findAll {
            it.domainType == 'EnumerationType'
        }.size() == 1

        dataModel1.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.size() == 2

        dataModel1.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.size() == 2

        dataModel1.dataTypes.find {
            it.domainType == 'EnumerationType'
        }.enumerationValues.key.sort() == ['N', 'Y']


    }

}