package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileRequestDTO

import groovy.transform.CompileStatic
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class SemanticEmbeddingProfileAdministrationService implements SemanticProfileAdministrationService {

    private final SemanticRepository semanticRepository
    private final EmbeddingProviderRegistry embeddingProviderRegistry

    SemanticEmbeddingProfileAdministrationService(SemanticRepository semanticRepository,
                                                 EmbeddingProviderRegistry embeddingProviderRegistry) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
    }

    @Connectable
    @Override
    List<SemanticEmbeddingProfileDTO> profiles() {
        semanticRepository.profiles().collect {Map<String, Object> profile -> SemanticEmbeddingProfileDTO.fromMap(profile)} as List<SemanticEmbeddingProfileDTO>
    }

    @Connectable
    @Override
    SemanticEmbeddingProfileDTO createProfile(SemanticEmbeddingProfileRequestDTO request) {
        Map<String, Object> safeRequest = request == null ?
            new LinkedHashMap<String, Object>() :
            new LinkedHashMap<String, Object>(request.toMap())
        String embeddingModel = stringValue(safeRequest, 'embeddingModel') ?: stringValue(safeRequest, 'model')
        if (embeddingModel != null && !embeddingModel.trim().isEmpty()) {
            safeRequest.put('embeddingModel', embeddingModel.trim())
        }
        boolean dimensionInferred = false
        if (safeRequest.get('dimension') == null) {
            String provider = requiredString(safeRequest, 'provider')
            String model = requiredString(safeRequest, 'embeddingModel')
            try {
                safeRequest.put('dimension', embeddingProviderRegistry.dimensionFor(provider, model))
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not infer embedding dimension for ${provider}/${model}: ${e.message}", e)
            }
            dimensionInferred = true
        }
        Map<String, Object> profile = semanticRepository.createProfile(safeRequest)
        if (dimensionInferred) {
            profile.put('dimensionInferred', true)
        }
        SemanticEmbeddingProfileDTO.fromMap(profile)
    }

    @Connectable
    @Override
    SemanticEmbeddingProfileDTO deleteProfile(String profileName) {
        assertProfileExists(profileName)
        SemanticEmbeddingProfileDTO.fromMap(semanticRepository.deleteProfile(profileName))
    }

    @Connectable
    @Override
    SemanticEmbeddingProfileDTO enable(String profileName) {
        SemanticEmbeddingProfileDTO.fromMap(semanticRepository.setProfileEnabled(profileName, true))
    }

    @Connectable
    @Override
    SemanticEmbeddingProfileDTO disable(String profileName) {
        SemanticEmbeddingProfileDTO.fromMap(semanticRepository.setProfileEnabled(profileName, false))
    }

    @Connectable
    @Override
    SemanticCorpusDTO deleteChunksForCorpus(String corpusName) {
        SemanticCorpusDTO.fromMap(semanticRepository.deleteChunksForCorpus(corpusName))
    }

    private void assertProfileExists(String profileName) {
        if (semanticRepository.profiles().find {Map<String, Object> profile -> profile.get('name') == profileName} == null) {
            throw new IllegalArgumentException("No semantic embedding profile named ${profileName}")
        }
    }

    private static String requiredString(Map<String, Object> request, String key) {
        String value = stringValue(request, key)
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field ${key}")
        }
        value.trim()
    }

    private static String stringValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        value == null ? null : String.valueOf(value)
    }
}
