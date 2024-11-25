package uk.ac.ox.softeng.mauro.test.domain.facet

import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.test.domain.TestModelData

import spock.lang.Specification

class RuleSpec extends Specification {


    void 'clone -should clone new rule instance '() {
        given:
        Rule original = TestModelData.testRule
        original.ruleRepresentations = [
                new RuleRepresentation().tap {
                    id = UUID.randomUUID()
                    language = 'test language 1'
                    representation = 'test representation 1'
                },
                new RuleRepresentation().tap {
                    id = UUID.randomUUID()
                    language = 'test language 2'
                    representation = 'test representation 2'
                } ]

        when:
        Rule cloned = original.clone()
        then:

        //assert clone works as per groovy docs
        !cloned.is(original)
        !cloned.ruleRepresentations.is(original.ruleRepresentations)
        cloned.id.is(original.id)
        cloned.name.is(original.name)
        cloned.description.is(original.description)
    }


}