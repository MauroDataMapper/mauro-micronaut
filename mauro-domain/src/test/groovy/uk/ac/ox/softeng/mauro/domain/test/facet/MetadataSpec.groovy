package uk.ac.ox.softeng.mauro.domain.test.facet

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.facet.AnnotationService

import java.text.Annotation

/**
 * Tests for Annotation Service
 */
class MetadataSpec extends Specification {

    void "Test metadata as map"() {
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

}