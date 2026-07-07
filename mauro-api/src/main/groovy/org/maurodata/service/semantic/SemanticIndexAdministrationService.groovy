package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import org.reactivestreams.Publisher

@CompileStatic
interface SemanticIndexAdministrationService {

    Map<String, Object> rebuildCatalogueIndex(String indexName,
                                              String corpusName,
                                              List<String> domainTypes,
                                              UUID mauroModelId,
                                              Integer maxRows,
                                              Integer batchSize,
                                              boolean force)

    List<Map<String, Object>> reconcileDeclaredIndexes()

    boolean hasEmbeddings(String indexName)

    boolean hasEmbeddings(String indexName, UUID mauroModelId)

    Map<String, Object> indexingStatus()

    Map<String, Object> setIndexingEnabled(boolean enabled)

    boolean autoReconcileEnabled()

    Map<String, Object> setAutoReconcileEnabled(boolean enabled)

    List<Map<String, Object>> corpora()

    Map<String, Object> createCorpus(Map<String, Object> request)

    List<Map<String, Object>> modelIndexes()

    List<Map<String, Object>> modelIndexStats(UUID mauroModelId)

    Map<String, Object> createModelIndex(Map<String, Object> request)

    Map<String, Object> deleteModelIndex(UUID mauroModelId, String profileName)

    Map<String, Object> startModelIndexJob(UUID mauroModelId, String profileName, Map<String, Object> request)

    List<Map<String, Object>> jobs(boolean includeHistory)

    Map<String, Object> jobStatus(UUID jobId)

    Map<String, Object> cancelJob(UUID jobId)

    Map<String, Object> resumeJob(UUID jobId)

    String jobEvents(UUID jobId)

    Publisher<String> followJobEvents(UUID jobId, Long afterSequence)

    List<Map<String, Object>> recoverInterruptedJobs()
}
