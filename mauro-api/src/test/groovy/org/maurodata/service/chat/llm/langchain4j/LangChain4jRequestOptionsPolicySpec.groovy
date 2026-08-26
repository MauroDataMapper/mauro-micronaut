package org.maurodata.service.chat.llm.langchain4j

import spock.lang.Specification

class LangChain4jRequestOptionsPolicySpec extends Specification {

    void 'openai gpt 5 models omit sampling controls that default-only models reject'() {
        when:
        Map<String, Object> options = LangChain4jRequestOptionsPolicy.sanitize('openai', 'gpt-5.6-luna', [
            purpose: 'agent_context_resolver',
            temperature: 0.3d,
            topP: 0.9d,
            top_p: 0.8d,
            maxOutputTokens: 768
        ] as Map<String, Object>)

        then:
        !options.containsKey('temperature')
        !options.containsKey('topP')
        !options.containsKey('top_p')
        options.purpose == 'agent_context_resolver'
        options.maxOutputTokens == 768
    }

    void 'openai o series models omit sampling controls'() {
        when:
        Map<String, Object> options = LangChain4jRequestOptionsPolicy.sanitize('openai', 'o4-mini', [
            temperature: 0.0d,
            top_p: 1.0d
        ] as Map<String, Object>)

        then:
        options.isEmpty()
    }

    void 'non default-only models keep sampling controls'() {
        expect:
        LangChain4jRequestOptionsPolicy.sanitize(providerType, modelName, [temperature: 0.2d, top_p: 0.8d] as Map<String, Object>) ==
            [temperature: 0.2d, top_p: 0.8d]

        where:
        providerType | modelName
        'openai'     | 'gpt-4o-mini'
        'ollama'     | 'gpt-5'
    }
}
