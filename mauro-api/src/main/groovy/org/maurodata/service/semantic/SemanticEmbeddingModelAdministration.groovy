package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import org.maurodata.domain.search.dto.SemanticEmbeddingModelPullResponseDTO

@CompileStatic
interface SemanticEmbeddingModelAdministration {

    SemanticEmbeddingModelPullResponseDTO pull(String provider, String model)
}
