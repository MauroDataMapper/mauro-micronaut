package org.maurodata.service.semantic

import groovy.transform.CompileStatic

@CompileStatic
class EmbeddingProfile {
    UUID id
    String name
    String provider
    String embeddingModel
    Integer dimension
    String distanceMetric
    String description
}
