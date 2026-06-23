package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class SemanticEmbeddingModelAdministrationService implements SemanticEmbeddingModelAdministration {

    private final OllamaModelAdministrationService ollamaModelAdministrationService

    SemanticEmbeddingModelAdministrationService(OllamaModelAdministrationService ollamaModelAdministrationService) {
        this.ollamaModelAdministrationService = ollamaModelAdministrationService
    }

    @Override
    Map<String, Object> pull(String provider, String model) {
        String cleanProvider = provider == null || provider.trim().isEmpty() ? 'ollama' : provider.trim()
        switch (cleanProvider) {
            case 'ollama':
                return ollamaModelAdministrationService.pull(model)
            default:
                throw new IllegalArgumentException("Unsupported embedding model provider ${cleanProvider}")
        }
    }
}
