package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic
import org.reactivestreams.Publisher

@CompileStatic
interface LlmProvider {
    String id()
    Publisher<ProviderChunk> streamChat(ProviderRequest request)
}
