package org.maurodata.service.chat.llm

import spock.lang.Specification

class OllamaTextToolCallExtractorSpec extends Specification {

    void 'extracts Ministral tool call prefix shape'() {
        given:
        OllamaTextToolCallExtractor extractor = new OllamaTextToolCallExtractor()

        when:
        OllamaTextToolCallExtractor.ExtractedToolCall call = extractor.extract(
            '[TOOL_CALLS]mauro_skill{"id": "mauro-form-representation"}',
            ['mauro_skill', 'mauro_keyword_search'] as Set<String>
        )

        then:
        call != null
        call.name == 'mauro_skill'
        call.parameters == [id: 'mauro-form-representation']
        call.rawJson == '{"id": "mauro-form-representation"}'
    }

    void 'ignores prefixed tool calls for disallowed tools'() {
        given:
        OllamaTextToolCallExtractor extractor = new OllamaTextToolCallExtractor()

        expect:
        extractor.extract(
            '[TOOL_CALLS]unknown_tool{"id": "mauro-form-representation"}',
            ['mauro_skill'] as Set<String>
        ) == null
    }

    void 'detects text tool call marker'() {
        expect:
        OllamaTextToolCallExtractor.looksLikeTextToolCall('[TOOL_CALLS]mauro_skill{"id":"x"}')
        OllamaTextToolCallExtractor.looksLikeTextToolCall(' [tool_calls]mauro_skill{"id":"x"}')
        OllamaTextToolCallExtractor.looksLikeTextToolCall('Here are results.\n[TOOL_CALLS]mauro_get{"uri":"x"}')
        !OllamaTextToolCallExtractor.looksLikeTextToolCall('Here are some results')
    }

    void 'extracts tool call marker appended after visible prose'() {
        given:
        OllamaTextToolCallExtractor extractor = new OllamaTextToolCallExtractor()

        when:
        OllamaTextToolCallExtractor.ExtractedToolCall call = extractor.extract(
            'Here are results.\n[TOOL_CALLS]mauro_get{"uri":"mauro-api://http-get/api/dataModels/dm-1"}',
            ['mauro_get', 'mauro_keyword_search'] as Set<String>
        )

        then:
        call != null
        call.name == 'mauro_get'
        call.parameters == [uri: 'mauro-api://http-get/api/dataModels/dm-1']
        call.rawJson == '{"uri":"mauro-api://http-get/api/dataModels/dm-1"}'
    }
}
