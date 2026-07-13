package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
final class ToolLoopGuards {

    static final int MAX_TOOL_ROUNDS = 3
    static final int MAX_TOOL_ERRORS = 2

    private ToolLoopGuards() {
    }

    static boolean shouldContinueToolLoop(
        final int toolRound,
        final int toolErrors,
        final int successfulToolExecutions,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final String messageId
    ) {
        if (toolErrors >= MAX_TOOL_ERRORS) {
            sink.next(new ProviderChunk(
                'error',
                messageId,
                'Provider stopped: too many tool-call errors.',
                Collections.<String, Object>emptyMap()
            ))
            return false
        }

        if (successfulToolExecutions <= 0) {
            return false
        }

        if (toolRound > MAX_TOOL_ROUNDS) {
            sink.next(new ProviderChunk(
                'error',
                messageId,
                'Provider stopped: reached maximum tool-call rounds.',
                Collections.<String, Object>emptyMap()
            ))
            return false
        }

        return true
    }
}
