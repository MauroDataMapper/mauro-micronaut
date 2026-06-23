package org.maurodata.service.semantic

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class SemanticCandidate {
    UUID chunkId
    UUID sourceId
    String sourceDomainType
    String sourceLabel
    String description
    String chunkKind
    Integer chunkOrdinal
    String sourceText
    String embeddingProfile
    Double distance
    Double similarity
    Instant dateCreated
    Instant lastUpdated
}
