package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class SemanticChunkMatchDTO {

    UUID chunkId
    String chunkKind
    String chunkGroup
    Integer chunkOrdinal
    String matchedSourceDomainType
    String matchedSourceLabel
    String sourceText
    String embeddingProfile
    Double significanceWeight
    Double weightedSimilarity
    Double distance
    Double similarity
    Instant indexedAt
}
