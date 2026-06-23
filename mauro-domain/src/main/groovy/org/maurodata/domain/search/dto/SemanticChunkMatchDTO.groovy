package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class SemanticChunkMatchDTO {

    UUID chunkId
    String chunkKind
    Integer chunkOrdinal
    String sourceText
    String embeddingProfile
    Double distance
    Double similarity
    Instant indexedAt
}
