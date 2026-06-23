package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticProfileAdministrationService)
class NoOpSemanticProfileAdministrationService implements SemanticProfileAdministrationService {

    static final String REASON = 'semantic profile administration implementation is not installed'

    @Override
    List<Map<String, Object>> profiles() {
        Collections.emptyList()
    }

    @Override
    List<Map<String, Object>> indexes() {
        Collections.emptyList()
    }

    @Override
    Map<String, Object> createIndex(Map<String, Object> request) {
        unavailable()
    }

    @Override
    Map<String, Object> deleteIndex(String indexName) {
        unavailable(indexName: indexName)
    }

    @Override
    Map<String, Object> createProfile(Map<String, Object> request) {
        unavailable()
    }

    @Override
    Map<String, Object> deleteProfile(String profileName) {
        unavailable(profileName: profileName)
    }

    @Override
    Map<String, Object> enable(String profileName) {
        unavailable(profileName: profileName)
    }

    @Override
    Map<String, Object> disable(String profileName) {
        unavailable(profileName: profileName)
    }

    @Override
    Map<String, Object> link(String indexName, String profileName) {
        unavailable(indexName: indexName, profileName: profileName)
    }

    @Override
    Map<String, Object> unlink(String indexName, String profileName) {
        unavailable(indexName: indexName, profileName: profileName)
    }

    @Override
    Map<String, Object> deleteEmbeddingsForIndex(String indexName) {
        unavailable(indexName: indexName)
    }

    @Override
    Map<String, Object> deleteChunksForCorpus(String corpusName) {
        unavailable(corpusName: corpusName)
    }

    private static Map<String, Object> unavailable(Map<String, Object> extra = Collections.<String, Object>emptyMap()) {
        Map<String, Object> result = [status: 'unavailable', reason: REASON] as Map<String, Object>
        result.putAll(extra)
        result
    }
}
