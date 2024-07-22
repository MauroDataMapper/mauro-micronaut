package uk.ac.ox.softeng.mauro.test.domain.terminology


import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

/**
 * TerminologySpec is a class for testing functionality relating to the Terminology class
 * @see Terminology
 */
class TerminologySpec extends Specification {

    static testTerminology = Terminology.build(label: 'ICD 10') {
        author "James Welch"
        description "The World Health Organization (WHO) International Classification of Diseases (ICD) is the global standard which categorises and reports diseases to compile health information related to deaths, illness or injury worldwide."
        id UUID.randomUUID()
        term {
            code "B15"
            definition "Acute hepatitis A"
            id UUID.randomUUID()
        }

        term(code: "B15.0", definition: "Hepatitis A with hepatic coma")

        term(code: "B15.9") {
            definition "Hepatitis A without hepatic coma"
            description "Hepatitis A (acute)(viral) NOS"
            id UUID.randomUUID()
        }

        termRelationshipType {
            label "BroaderThan"
            childRelationship false
            parentalRelationship true
            id UUID.randomUUID()
        }

        termRelationship {
            sourceTerm "B15"
            targetTerm "B15.0"
            relationshipType "BroaderThan"
            id UUID.randomUUID()
        }
        termRelationship {
            sourceTerm "B15"
            targetTerm "B15.9"
            relationshipType "BroaderThan"
        }
    }


    def "Test the DSL for creating objects"() {

        when:
        testTerminology
        then:
        testTerminology.terms.size() == 3
        testTerminology.termRelationshipTypes.size() == 1
        testTerminology.termRelationships.size() == 2

        testTerminology.termRelationships.each { termRelationship ->
            termRelationship.sourceTerm.code == "B15"
            ["B15.0", "B15.9"].contains(termRelationship.targetTerm.code)
        }
    }

    void 'clone -should clone new terminology instance with new associations -deep copy of terminology and all its owning objects'() {
        given:
        Terminology original = testTerminology
        when:
        Terminology cloned = testTerminology.clone()
        then:

        //clone clones entire object, all fields, including 'id'
        !cloned.is(original)

        cloned.terms == original.terms
        !cloned.terms.is(original.terms)

        cloned.termRelationshipTypes == original.termRelationshipTypes
        !cloned.termRelationshipTypes.is(original.termRelationshipTypes)

        cloned.termRelationships == original.termRelationships
        !cloned.termRelationships.is(original.termRelationships)

        ObjectDiff diff = cloned.diff(original)
        diff.numberOfDiffs == 0
    }

}