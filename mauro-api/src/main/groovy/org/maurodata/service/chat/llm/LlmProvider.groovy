package org.maurodata.service.chat.llm

import org.reactivestreams.Publisher

interface LlmProvider {
    String id()
    Publisher<ProviderChunk> streamChat(ProviderRequest request)
}
