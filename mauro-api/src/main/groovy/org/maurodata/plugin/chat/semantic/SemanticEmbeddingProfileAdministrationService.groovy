package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class SemanticEmbeddingProfileAdministrationService implements SemanticProfileAdministrationService {

    private final SemanticRepository semanticRepository

    SemanticEmbeddingProfileAdministrationService(SemanticRepository semanticRepository) {
        this.semanticRepository = semanticRepository
    }

    @Connectable
    @Override
    List<Map<String, Object>> profiles() {
        semanticRepository.profiles()
    }

    @Connectable
    @Override
    List<Map<String, Object>> indexes() {
        semanticRepository.indexes()
    }

    @Connectable
    @Override
    Map<String, Object> createIndex(Map<String, Object> request) {
        semanticRepository.createIndex(requiredString(request, 'name'), stringValue(request, 'corpusName') ?: 'catalogue-items')
    }

    @Connectable
    @Override
    Map<String, Object> deleteIndex(String indexName) {
        assertIndexExists(indexName)
        semanticRepository.deleteIndex(indexName)
    }

    @Connectable
    @Override
    Map<String, Object> createProfile(Map<String, Object> request) {
        semanticRepository.createProfile(request)
    }

    @Connectable
    @Override
    Map<String, Object> deleteProfile(String profileName) {
        assertProfileExists(profileName)
        semanticRepository.deleteProfile(profileName)
    }

    @Connectable
    @Override
    Map<String, Object> enable(String profileName) {
        semanticRepository.setProfileEnabled(profileName, true)
    }

    @Connectable
    @Override
    Map<String, Object> disable(String profileName) {
        semanticRepository.setProfileEnabled(profileName, false)
    }

    @Connectable
    @Override
    Map<String, Object> link(String indexName, String profileName) {
        assertIndexExists(indexName)
        assertProfileExists(profileName)
        Map<String, Object> result = semanticRepository.linkProfileToIndex(indexName, profileName)
        result.put('profile', semanticRepository.findProfileByName(profileName)?.name ?: profileName)
        result
    }

    @Connectable
    @Override
    Map<String, Object> unlink(String indexName, String profileName) {
        assertIndexExists(indexName)
        assertProfileExists(profileName)
        semanticRepository.unlinkProfileFromIndex(indexName, profileName)
    }

    @Connectable
    @Override
    Map<String, Object> deleteEmbeddingsForIndex(String indexName) {
        assertIndexExists(indexName)
        semanticRepository.deleteEmbeddingsForIndex(indexName)
    }

    @Connectable
    @Override
    Map<String, Object> deleteChunksForCorpus(String corpusName) {
        semanticRepository.deleteChunksForCorpus(corpusName)
    }

    private void assertIndexExists(String indexName) {
        if (!semanticRepository.indexExists(indexName)) {
            throw new IllegalArgumentException("No semantic index named ${indexName}")
        }
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
