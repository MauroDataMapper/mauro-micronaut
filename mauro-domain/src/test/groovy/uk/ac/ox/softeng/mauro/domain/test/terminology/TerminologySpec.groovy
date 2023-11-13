package uk.ac.ox.softeng.mauro.domain.test.terminology

import groovy.transform.CompileStatic
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

/**
 * TerminologySpec is a class for testing functionality relating to the Terminology class
 * @see Terminology
 */
class TerminologySpec extends Specification {

    def "Test the DSL for creating objects"() {

        when:
        Terminology terminology1 = Terminology.build (label: 'ICD 10') {
            author "James Welch"
            description "The World Health Organization (WHO) International Classification of Diseases (ICD) is the global standard which categorises and reports diseases to compile health information related to deaths, illness or injury worldwide."

            term {
                code "B15"
                definition "Acute hepatitis A"
            }

            term(code: "B15.0", definition: "Hepatitis A with hepatic coma")

            term(code: "B15.9") {
                definition "Hepatitis A without hepatic coma"
                description "Hepatitis A (acute)(viral) NOS"
            }

            termRelationshipType {
                label "BroaderThan"
                childRelationship false
                parentalRelationship true
            }

            termRelationship {
                sourceTerm"B15"
                targetTerm "B15.0"
                relationshipType "BroaderThan"
            }
            termRelationship {
                sourceTerm"B15"
                targetTerm "B15.9"
                relationshipType "BroaderThan"
            }
        }

        then:
        terminology1.terms.size() == 3
        terminology1.termRelationshipTypes.size() == 1
        terminology1.termRelationships.size() == 2

        terminology1.termRelationships.each {termRelationship ->
            termRelationship.sourceTerm.code == "B15"
            ["B15.0", "B15.9"].contains(termRelationship.targetTerm.code)
        }
    }

}