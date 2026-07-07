package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticIndexAdministrationService)
class NoOpSemanticIndexAdministrationService implements SemanticIndexAdministrationService {

    static final String REASON = 'semantic index implementation is not installed'

    @Override
    Map<String, Object> rebuildCatalogueIndex(String indexName,
                                              String corpusName,
                                              List<String> domainTypes,
                                              UUID mauroModelId,
                                              Integer maxRows,
                                              Integer batchSize,
                                              boolean force) {
        [
            indexName: indexName ?: 'catalogue-items-default',
            corpusName: corpusName ?: 'catalogue-items',
            status: 'unavailable',
            reason: REASON
        ] as Map<String, Object>
    }

    @Override
    List<Map<String, Object>> reconcileDeclaredIndexes() {
        Collections.emptyList()
    }

    @Override
    boolean hasEmbeddings(String indexName) {
        false
    }

    @Override
    boolean hasEmbeddings(String indexName, UUID mauroModelId) {
        false
    }

    @Override
    Map<String, Object> indexingStatus() {
        [enabled: false, autoReconcile: false, reason: REASON] as Map<String, Object>
    }

    @Override
    Map<String, Object> setIndexingEnabled(boolean enabled) {
        [enabled: false, requestedEnabled: enabled, reason: REASON] as Map<String, Object>
    }

    @Override
    boolean autoReconcileEnabled() {
        false
    }

    @Override
    Map<String, Object> setAutoReconcileEnabled(boolean enabled) {
        [enabled: false, autoReconcile: false, requestedAutoReconcile: enabled, reason: REASON] as Map<String, Object>
    }

    @Override
    List<Map<String, Object>> corpora() {
        Collections.emptyList()
    }

    @Override
    Map<String, Object> createCorpus(Map<String, Object> request) {
        [status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    List<Map<String, Object>> modelIndexes() {
        Collections.emptyList()
    }

    @Override
    List<Map<String, Object>> modelIndexStats(UUID mauroModelId) {
        Collections.emptyList()
    }

    @Override
    Map<String, Object> createModelIndex(Map<String, Object> request) {
        [status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    Map<String, Object> deleteModelIndex(UUID mauroModelId, String profileName) {
        [mauroModelId: mauroModelId?.toString(), profileName: profileName, deleted: 0, reason: REASON] as Map<String, Object>
    }

    @Override
    Map<String, Object> startModelIndexJob(UUID mauroModelId, String profileName, Map<String, Object> request) {
        [mauroModelId: mauroModelId?.toString(), profileName: profileName, status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    List<Map<String, Object>> jobs(boolean includeHistory) {
        Collections.emptyList()
    }

    @Override
    Map<String, Object> jobStatus(UUID jobId) {
        [jobId: jobId?.toString(), status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    Map<String, Object> cancelJob(UUID jobId) {
        [jobId: jobId?.toString(), status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    Map<String, Object> resumeJob(UUID jobId) {
        [jobId: jobId?.toString(), status: 'unavailable', reason: REASON] as Map<String, Object>
    }

    @Override
    String jobEvents(UUID jobId) {
        ''
    }

    @Override
    Publisher<String> followJobEvents(UUID jobId, Long afterSequence) {
        Flux.empty()
    }

    @Override
    List<Map<String, Object>> recoverInterruptedJobs() {
        Collections.emptyList()
    }
}
