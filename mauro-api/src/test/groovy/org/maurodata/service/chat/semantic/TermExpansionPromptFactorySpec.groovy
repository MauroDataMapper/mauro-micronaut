package org.maurodata.service.chat.semantic

import spock.lang.Specification

class TermExpansionPromptFactorySpec extends Specification {

    void 'builds all term expansion prompts'() {
        given:
        TermExpansionPromptFactory factory = new TermExpansionPromptFactory()

        when:
        List<TermExpansionPrompt> prompts = factory.prompts('diabetes', 'Mauro catalogue search', 'British')

        then:
        prompts*.category == [
            'synonym',
            'neighbours',
            'hypernyms',
            'hyponyms',
            'euphemisms',
            'alternativeExpressions',
            'searchKeywords',
            'operationalRules',
            'implicit',
            'usageSignals'
        ]
        prompts.every {TermExpansionPrompt prompt -> prompt.prompt.contains('diabetes')}
        prompts.every {TermExpansionPrompt prompt -> prompt.prompt.contains('Mauro catalogue search')}
    }
}
