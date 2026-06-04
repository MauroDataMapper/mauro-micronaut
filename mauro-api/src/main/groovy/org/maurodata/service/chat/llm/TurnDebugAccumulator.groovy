package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
final class TurnDebugAccumulator {
    int rawTokenChunks = 0
    int publishedTokenChunks = 0
    int filteredControlTokenChunks = 0
    int structuredToolCalls = 0
    int executableStructuredToolCalls = 0
    boolean fallbackAttempted = false
    boolean fallbackExtracted = false
    int successfulToolExecutions = 0
    int toolErrors = 0
    int assistantTextLength = 0

    TurnDebugSnapshot snapshot(final String sessionId, final String messageId) {
        return new TurnDebugSnapshot(
            sessionId,
            messageId,
            rawTokenChunks,
            publishedTokenChunks,
            filteredControlTokenChunks,
            structuredToolCalls,
            executableStructuredToolCalls,
            fallbackAttempted,
            fallbackExtracted,
            successfulToolExecutions,
            toolErrors,
            assistantTextLength
        )
    }
}
