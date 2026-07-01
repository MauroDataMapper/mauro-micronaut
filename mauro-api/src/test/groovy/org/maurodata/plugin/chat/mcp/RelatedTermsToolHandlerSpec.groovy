package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import org.maurodata.plugin.chat.mcp.*

import org.maurodata.plugin.chat.semantic.RelatedTermsApiService
import org.maurodata.plugin.chat.semantic.InMemorySemanticTermStore
import org.maurodata.plugin.chat.semantic.TermExpansionOutputParser
import org.maurodata.plugin.chat.semantic.TermExpansionPromptFactory
import org.maurodata.service.chat.semantic.TextGenerationService
import spock.lang.Specification

class RelatedTermsToolHandlerSpec extends Specification {

    void 'generates related terms when no stored terms exist'() {
        given:
        RelatedTermsToolHandler handler = new RelatedTermsToolHandler(
            new RelatedTermsApiService(
                new InMemorySemanticTermStore(),
                new TermExpansionPromptFactory(),
                new TermExpansionOutputParser(),
                new FakeTextGenerationService(),
                'Mauro catalogue search',
                'openai',
                'text-embedding-3-small',
                'llama3.2:1b',
                'synonym,neighbours',
                2
            )
        )

        when:
        Map<String, Object> result = handler.invoke([
            text: 'diabetes',
            max: 5
        ] as Map<String, Object>)

        then:
        result.inputText == 'diabetes'
        result.embeddingProvider == 'openai'
        result.embeddingModel == 'text-embedding-3-small'
        (result.terms as List<Map<String, Object>>)*.text.contains('diabetes mellitus')
        (result.terms as List<Map<String, Object>>)*.text.contains('blood glucose')
        result.prompts == []

        when:
        String modelText = handler.modelText(result)

        then:
        modelText.contains('Suggested Search Expression')
        modelText.contains('"diabetes mellitus"')
        modelText.contains('call mauro_search next')
    }

    static class FakeTextGenerationService implements TextGenerationService {
        @Override
        String generate(String model, String prompt) {
            '''
            - diabetes mellitus
            - blood glucose
            '''
        }
    }
}
