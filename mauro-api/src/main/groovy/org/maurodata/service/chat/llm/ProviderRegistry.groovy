package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class ProviderRegistry {

    private final Map<String, LlmProvider> providers

    ProviderRegistry(List<LlmProvider> providerList) {
        providers = providerList.collectEntries {[(it.id()): it]}
    }

    LlmProvider byId(String providerId) {
        LlmProvider provider = providers[providerId]
        if (!provider) {
            throw new IllegalArgumentException("Unknown provider: ${providerId}")
        }
        provider
    }

    LlmProvider byModel(String model) {
        if (model?.startsWith('gpt-')) {
            return byId('openai')
        }
        byId('ollama')
    }
}
