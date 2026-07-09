package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileRequestDTO

@CompileStatic
interface SemanticProfileAdministrationService {

    List<SemanticEmbeddingProfileDTO> profiles()

    SemanticEmbeddingProfileDTO createProfile(SemanticEmbeddingProfileRequestDTO request)

    SemanticEmbeddingProfileDTO deleteProfile(String profileName)

    SemanticEmbeddingProfileDTO enable(String profileName)

    SemanticEmbeddingProfileDTO disable(String profileName)

    SemanticCorpusDTO deleteChunksForCorpus(String corpusName)
}
