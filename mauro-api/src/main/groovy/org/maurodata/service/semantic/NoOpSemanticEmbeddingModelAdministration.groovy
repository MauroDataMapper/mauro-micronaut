package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticEmbeddingModelAdministration)
class NoOpSemanticEmbeddingModelAdministration implements SemanticEmbeddingModelAdministration {

    @Override
    Map<String, Object> pull(String provider, String model) {
        [
            status: 'unavailable',
            provider: provider,
            model: model,
            reason: 'embedding model administration implementation is not installed'
        ] as Map<String, Object>
    }
}
