package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*
import org.maurodata.domain.search.dto.SemanticEmbeddingModelPullResponseDTO

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
    SemanticEmbeddingModelPullResponseDTO pull(String provider, String model) {
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
        SemanticEmbeddingModelPullResponseDTO.fromMap(result)
    }

    private Map<String, Object> probeEmbeddingDimension(String provider, String model) {
        try {
            [
                dimension: embeddingProviderRegistry.dimensionFor(provider, model),
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
