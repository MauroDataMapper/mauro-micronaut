package uk.ac.ox.softeng.mauro.test.domain.facet


import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement

/**
 * Tests for Annotation Service
 */
class MetadataSpec extends Specification {

    def "Test retrieving metadata as map"() {
        when:
        DataElement dataElement = DataElement.build {
            label "My test data element"
            description "Test retrieving the metadata as a map"
            metadata("com.test1", "key1", "value1")
            metadata("com.test1", "key2", "value2")
            metadata("com.test2", "key3", "value3")
        }


        then:
        dataElement.metadataAsMap("com.test1") ==
                ["key1": "value1", "key2": "value2"]
        dataElement.metadataAsMap("com.test2") ==
                ["key3": "value3"]
        dataElement.metadataAsMap("com.test3") ==
                [:]

    }

    def "Test adding metadata as a map"() {
        when:
        DataElement dataElement = DataElement.build {
            label "My test data element"
            description "Test retrieving the metadata as a map"
            metadata("com.test1", ["key1": "value1", "key2": "value2"])
            metadata("com.test2", ["key3": "value3"])
            metadata("com.test3", [:])

        }
        then:
        dataElement.metadata.size() == 3
        dataElement.metadataAsMap("com.test1") ==
                ["key1": "value1", "key2": "value2"]
        dataElement.metadataAsMap("com.test2") ==
                ["key3": "value3"]
        dataElement.metadataAsMap("com.test3") ==
                [:]
    }

}