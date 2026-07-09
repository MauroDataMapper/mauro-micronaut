package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileRequestDTO

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticProfileAdministrationService)
class NoOpSemanticProfileAdministrationService implements SemanticProfileAdministrationService {

    static final String REASON = 'semantic profile administration implementation is not installed'

    @Override
    List<SemanticEmbeddingProfileDTO> profiles() {
        Collections.emptyList()
    }

    @Override
    SemanticEmbeddingProfileDTO createProfile(SemanticEmbeddingProfileRequestDTO request) {
        unavailableProfile(request?.name)
    }

    @Override
    SemanticEmbeddingProfileDTO deleteProfile(String profileName) {
        unavailableProfile(profileName)
    }

    @Override
    SemanticEmbeddingProfileDTO enable(String profileName) {
        unavailableProfile(profileName)
    }

    @Override
    SemanticEmbeddingProfileDTO disable(String profileName) {
        unavailableProfile(profileName)
    }

    @Override
    SemanticCorpusDTO deleteChunksForCorpus(String corpusName) {
        SemanticCorpusDTO.fromMap([name: corpusName, enabled: false, description: REASON] as Map<String, Object>)
    }

    private static SemanticEmbeddingProfileDTO unavailableProfile(String profileName) {
        SemanticEmbeddingProfileDTO.fromMap([name: profileName, enabled: false, description: REASON] as Map<String, Object>)
    }
}
