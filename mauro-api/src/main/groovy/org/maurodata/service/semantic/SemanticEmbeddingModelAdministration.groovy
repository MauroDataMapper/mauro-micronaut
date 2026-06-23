package org.maurodata.service.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface SemanticEmbeddingModelAdministration {

    Map<String, Object> pull(String provider, String model)
}
