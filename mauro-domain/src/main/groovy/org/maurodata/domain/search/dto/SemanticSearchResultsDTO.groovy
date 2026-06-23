package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class SemanticSearchResultsDTO extends SearchResultsDTO {

    Double semanticScore
    Double rerankScore
    Integer matchedChunkCount
    List<String> embeddingProfiles = []
    List<SemanticChunkMatchDTO> chunks = []
}
