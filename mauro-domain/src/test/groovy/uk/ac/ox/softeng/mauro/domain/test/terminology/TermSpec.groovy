package uk.ac.ox.softeng.mauro.domain.test.terminology

import groovy.transform.CompileStatic
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.terminology.Term

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
}