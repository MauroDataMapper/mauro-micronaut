package uk.ac.ox.softeng.mauro.domain.test.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
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
            dataType {
                label 'string'
                description 'character string'
            }
            dataType {
                label 'integer'
                description 'a whole number, may be positive or negative, with no maximum or minimum'
            }

        }

        then:
        dataModel1.dataTypes.size() == 2

    }

}