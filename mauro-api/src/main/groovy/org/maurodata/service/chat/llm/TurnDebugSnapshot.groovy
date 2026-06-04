package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
final class TurnDebugSnapshot {
    final String sessionId
    final String messageId
    final int rawTokenChunks
    final int publishedTokenChunks
    final int filteredControlTokenChunks
    final int structuredToolCalls
    final int executableStructuredToolCalls
    final boolean fallbackAttempted
    final boolean fallbackExtracted
    final int successfulToolExecutions
    final int toolErrors
    final int assistantTextLength

    TurnDebugSnapshot(
        final String sessionId,
        final String messageId,
        final int rawTokenChunks,
        final int publishedTokenChunks,
        final int filteredControlTokenChunks,
        final int structuredToolCalls,
        final int executableStructuredToolCalls,
        final boolean fallbackAttempted,
        final boolean fallbackExtracted,
        final int successfulToolExecutions,
        final int toolErrors,
        final int assistantTextLength
    ) {
        this.sessionId = sessionId
        this.messageId = messageId
        this.rawTokenChunks = rawTokenChunks
        this.publishedTokenChunks = publishedTokenChunks
        this.filteredControlTokenChunks = filteredControlTokenChunks
        this.structuredToolCalls = structuredToolCalls
        this.executableStructuredToolCalls = executableStructuredToolCalls
        this.fallbackAttempted = fallbackAttempted
        this.fallbackExtracted = fallbackExtracted
        this.successfulToolExecutions = successfulToolExecutions
        this.toolErrors = toolErrors
        this.assistantTextLength = assistantTextLength
    }
}
