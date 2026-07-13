package org.maurodata.service.chat.llm

import spock.lang.Specification

class ToolCallAccumulatorSpec extends Specification {

    void 'completeAll keeps distinct call ids even when streamed with same local index'() {
        given:
        ToolCallAccumulator accumulator = new ToolCallAccumulator()

        when:
        accumulator.applyDelta(
            0,
            'call_one',
            'function',
            'mauro_terms',
            '{"text":"Pre-Transplant Assessment"}'
        )
        accumulator.applyDelta(
            0,
            'call_two',
            'function',
            'mauro_terms',
            '{"text":"Transplant Admission"}'
        )
        List<ToolCallAccumulator.CompletedToolCall> calls = accumulator.completeAll()

        then:
        calls*.callId == ['call_one', 'call_two']
        calls*.functionName == ['mauro_terms', 'mauro_terms']
        calls*.argumentsJson*.text == ['Pre-Transplant Assessment', 'Transplant Admission']
    }
}
