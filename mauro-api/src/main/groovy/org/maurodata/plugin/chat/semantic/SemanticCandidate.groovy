package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class SemanticCandidate {
    UUID chunkId
    UUID sourceId
    String sourceDomainType
    String sourceLabel
    UUID targetId
    String targetDomainType
    String targetLabel
    String targetDescription
    Integer relationDistance
    String description
    String chunkKind
    String chunkGroup
    Integer chunkOrdinal
    String sourceText
    String embeddingProfile
    Double distance
    Double similarity
    Instant dateCreated
    Instant lastUpdated
}
