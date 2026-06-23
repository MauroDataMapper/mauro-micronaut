package org.maurodata.service.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface SemanticProfileAdministrationService {

    List<Map<String, Object>> profiles()

    List<Map<String, Object>> indexes()

    Map<String, Object> createIndex(Map<String, Object> request)

    Map<String, Object> deleteIndex(String indexName)

    Map<String, Object> createProfile(Map<String, Object> request)

    Map<String, Object> deleteProfile(String profileName)

    Map<String, Object> enable(String profileName)

    Map<String, Object> disable(String profileName)

    Map<String, Object> link(String indexName, String profileName)

    Map<String, Object> unlink(String indexName, String profileName)

    Map<String, Object> deleteEmbeddingsForIndex(String indexName)

    Map<String, Object> deleteChunksForCorpus(String corpusName)
}
