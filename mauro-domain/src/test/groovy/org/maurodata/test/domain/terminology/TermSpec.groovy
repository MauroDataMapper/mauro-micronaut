package org.maurodata.test.domain.terminology

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.maurodata.domain.terminology.Terminology
import spock.lang.Specification
import org.maurodata.domain.terminology.Term

/**
 * TermSpec is a class for testing functionality relating to the Term class
 * @see Term
 */
class TermSpec extends Specification {

    def "Test the DSL for creating objects"() {

        when:

        Term term1 = Term.build(code: "B15", definition: "Acute hepatitis A")
        Term term2 = Term.build(code: "B15") {
            definition "Acute hepatitis A"
        }
        Term term3 = Term.build {
            code "B15"
            definition "Acute hepatitis A"
        }

        then:

        term1.code == term2.code
        term2.code == term3.code

        term1.definition == term2.definition
        term2.definition == term3.definition

    }

    def "Test deserialization from JSON"() {
        given:
        String json = """[{
            "label": "Test Terminology",
            "terms": [
                {"code": "A01", "definition": "Test Term A01_0"},
                {"code": "B02", "definition": "Test Term B02_0"}
            ],
            "termRelationshipTypes": [
                { "label": "is a", "description": "Indicates a hierarchical relationship" }
            ],
            "termRelationships":     [       
                {
                    "sourceTerm": "A01",
                    "targetTerm": "B02",
                    "relationshipType": "is a"
                }
            ]
        },
        {
            "label": "Test Terminology 2",
            "terms": [
                {"code": "A01", "definition": "Test Term A01_0"},
                {"code": "B02", "definition": "Test Term B02_0"}
            ],
            "termRelationshipTypes": [
                { "label": "is a", "description": "Indicates a hierarchical relationship" }
            ],
            "termRelationships":     [       
                {
                    "sourceTerm": "A01",
                    "targetTerm": "B02",
                    "relationshipType": "is a"
                }
            ]
        }]"""

        ObjectMapper objectMapper = new ObjectMapper()
        when:
        List<Terminology> terminologies = objectMapper.readValue(json, new TypeReference<List<Terminology>>() {})
        then:
        System.err.println(terminologies[0].label)
        System.err.println(terminologies[0].terms.size())
        Term a01_0 = terminologies[0].terms.find {it.code == "A01"}
        Term b02_0 = terminologies[0].terms.find {it.code == "B02"}
        assert terminologies[0].termRelationships[0].sourceTerm == a01_0
        assert terminologies[0].termRelationships[0].targetTerm == b02_0
        assert terminologies[0].termRelationships[0].relationshipType.label == "is a"
        assert terminologies[0].termRelationships[0].relationshipType == terminologies[0].termRelationshipTypes[0]

        System.err.println(terminologies[1].label)
        System.err.println(terminologies[1].terms.size())
        Term a01_1 = terminologies[1].terms.find {it.code == "A01"}
        Term b02_1 = terminologies[1].terms.find {it.code == "B02"}
        assert terminologies[1].termRelationships[0].sourceTerm == a01_1
        assert terminologies[1].termRelationships[0].targetTerm == b02_1
        assert terminologies[1].termRelationships[0].relationshipType.label == "is a"
        assert terminologies[1].termRelationships[0].relationshipType == terminologies[1].termRelationshipTypes[0]

        assert !a01_1.is(a01_0)
        assert !b02_1.is(b02_0)

    }
}