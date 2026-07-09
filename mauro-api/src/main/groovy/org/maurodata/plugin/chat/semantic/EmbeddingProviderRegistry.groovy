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

    EmbeddingProvider providerFor(String providerId, String embeddingModel) {
        providerFor(new EmbeddingProfile(provider: providerId, embeddingModel: embeddingModel))
    }

    int dimensionFor(String providerId, String embeddingModel) {
        EmbeddingProvider provider = providerFor(providerId, embeddingModel)
        EmbeddingProfile probeProfile = new EmbeddingProfile(
            provider: providerId,
            embeddingModel: embeddingModel,
            distanceMetric: 'cosine'
        )
        List<float[]> embeddings = provider.embed(probeProfile, ['dimension probe'] as List<String>)
        float[] vector = embeddings ? embeddings.first() : null
        if (vector == null) {
            throw new IllegalStateException('embedding provider returned no vector')
        }
        vector.length
    }
}
