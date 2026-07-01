package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class SemanticChunk {
    UUID id
    UUID corpusId
    String sourceType
    UUID sourceId
    String sourceDomainType
    String sourceLabel
    UUID mauroModelId
    String chunkKind
    String chunkGroup
    Integer chunkOrdinal
    String sourceText
    String contentHash
    Instant dateCreated
    Instant lastUpdated
}
