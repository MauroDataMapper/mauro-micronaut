package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.service.chat.llm.config.ChatProviderConfigurationResolver

@Singleton
@CompileStatic
class ProviderRegistry {

    private final Map<String, LlmProvider> providers
    private final ChatProviderConfigurationResolver configurationResolver

    ProviderRegistry(List<LlmProvider> providerList) {
        this(providerList, null)
    }

    @Inject
    ProviderRegistry(List<LlmProvider> providerList, ChatProviderConfigurationResolver configurationResolver) {
        providers = providerList.collectEntries {[(it.id()): it]}
        this.configurationResolver = configurationResolver
    }

    LlmProvider byId(String providerId) {
        LlmProvider provider = providers[providerId]
        if (!provider) {
            throw new IllegalArgumentException("Unknown provider: ${providerId}")
        }
        provider
    }

    LlmProvider byModel(String model) {
        if (model?.startsWith('gpt-') || model?.startsWith('o') || configuredForProvider('openai', model)) {
            return byId('openai')
        }
        byId('ollama')
    }

    private boolean configuredForProvider(String providerId, String model) {
        if (configurationResolver == null || model == null || !providers.containsKey(providerId)) {
            return false
        }
        configurationResolver.resolve(providerId).modelAllowlist?.contains(model)
    }
}
