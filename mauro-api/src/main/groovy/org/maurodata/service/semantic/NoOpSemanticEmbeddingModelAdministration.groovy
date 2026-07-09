package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SemanticEmbeddingModelPullResponseDTO

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticEmbeddingModelAdministration)
class NoOpSemanticEmbeddingModelAdministration implements SemanticEmbeddingModelAdministration {

    @Override
    SemanticEmbeddingModelPullResponseDTO pull(String provider, String model) {
        SemanticEmbeddingModelPullResponseDTO.fromMap([
            status: 'unavailable',
            provider: provider,
            model: model,
            message: 'embedding model administration implementation is not installed'
        ] as Map<String, Object>)
    }
}
