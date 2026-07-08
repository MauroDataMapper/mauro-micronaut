package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class SemanticEmbeddingModelAdministrationService implements SemanticEmbeddingModelAdministration {

    private final OllamaModelAdministrationService ollamaModelAdministrationService
    private final EmbeddingProviderRegistry embeddingProviderRegistry

    SemanticEmbeddingModelAdministrationService(OllamaModelAdministrationService ollamaModelAdministrationService,
                                               EmbeddingProviderRegistry embeddingProviderRegistry) {
        this.ollamaModelAdministrationService = ollamaModelAdministrationService
        this.embeddingProviderRegistry = embeddingProviderRegistry
    }

    @Override
    Map<String, Object> pull(String provider, String model) {
        String cleanProvider = provider == null || provider.trim().isEmpty() ? 'ollama' : provider.trim()
        String cleanModel = model == null ? null : model.trim()
        Map<String, Object> result
        switch (cleanProvider) {
            case 'ollama':
                result = ollamaModelAdministrationService.pull(cleanModel)
                break
            default:
                throw new IllegalArgumentException("Unsupported embedding model provider ${cleanProvider}")
        }
        result.putAll(probeEmbeddingDimension(cleanProvider, cleanModel))
        result
    }

    private Map<String, Object> probeEmbeddingDimension(String provider, String model) {
        try {
            EmbeddingProvider embeddingProvider = embeddingProviderRegistry.providerFor(provider, model)
            EmbeddingProfile probeProfile = new EmbeddingProfile(
                provider: provider,
                embeddingModel: model,
                distanceMetric: 'cosine'
            )
            List<float[]> embeddings = embeddingProvider.embed(probeProfile, ['dimension probe'] as List<String>)
            float[] vector = embeddings ? embeddings.first() : null
            if (vector == null) {
                return [
                    embeddingProbeSucceeded: false,
                    embeddingProbeError: 'embedding provider returned no vector'
                ] as Map<String, Object>
            }
            [
                dimension: vector.length,
                embeddingProbeSucceeded: true
            ] as Map<String, Object>
        } catch (Throwable t) {
            [
                embeddingProbeSucceeded: false,
                embeddingProbeError: t.message ?: t.class.name
            ] as Map<String, Object>
        }
    }
}
