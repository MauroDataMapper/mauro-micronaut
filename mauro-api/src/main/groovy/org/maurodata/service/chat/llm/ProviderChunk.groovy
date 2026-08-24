package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
final class ProviderChunk {
    final String type
    final String messageId
    final String content
    final Map<String, Object> metadata
    final ProviderError error

    ProviderChunk(String type, String messageId, String content, Map<String, Object> metadata) {
        this(type, messageId, content, metadata, null)
    }

    ProviderChunk(String type, String messageId, String content, Map<String, Object> metadata, ProviderError error) {
        this.type = type
        this.messageId = messageId
        this.content = content
        this.metadata = metadata ?: Collections.<String, Object>emptyMap()
        this.error = error
    }
}
