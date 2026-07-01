package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class SemanticSearchResultsDTO extends SearchResultsDTO {

    Double semanticScore
    Double rerankScore
    Double hybridScore
    Integer keywordRank
    Integer semanticRank
    Integer matchedChunkCount
    List<String> embeddingProfiles = []
    List<SemanticChunkMatchDTO> chunks = []
    List<String> evidence = []
    List<Map<String, Object>> evidenceDetails = []
}
