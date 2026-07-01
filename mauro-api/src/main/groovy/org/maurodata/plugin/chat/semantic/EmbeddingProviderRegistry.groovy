package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class EmbeddingProviderRegistry {

    private final List<EmbeddingProvider> providers

    EmbeddingProviderRegistry(List<EmbeddingProvider> providers) {
        this.providers = providers ?: []
    }

    EmbeddingProvider providerFor(EmbeddingProfile profile) {
        EmbeddingProvider provider = providers.find {EmbeddingProvider candidate -> candidate.supports(profile)}
        if (provider == null) {
            throw new IllegalStateException("No embedding provider available for ${profile?.provider}/${profile?.embeddingModel}")
        }
        provider
    }
}
