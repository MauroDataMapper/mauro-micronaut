package uk.ac.ox.softeng.mauro.domain.test

import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType

class TermRelationshipTypeSpec extends Specification{


    def "Test the DSL for creating objects"() {

        when:

        TermRelationshipType termRelationshipType1 = TermRelationshipType.build(
                label: "BroaderThan",
                parentalRelationship: true,
                childRelationship: false)
        TermRelationshipType termRelationshipType2 = TermRelationshipType.build(label: "BroaderThan") {
            parentalRelationship true
            childRelationship false
        }
        TermRelationshipType termRelationshipType3 = TermRelationshipType.build {
            label "BroaderThan"
            parentalRelationship true
            childRelationship false
        }

        then:

        termRelationshipType1.label == termRelationshipType2.label
        termRelationshipType2.label == termRelationshipType3.label
    }

}