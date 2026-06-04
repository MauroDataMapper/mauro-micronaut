package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor
final class ProviderChunk {
    final String type
    final String messageId
    final String content
    final Map<String, Object> metadata
}
