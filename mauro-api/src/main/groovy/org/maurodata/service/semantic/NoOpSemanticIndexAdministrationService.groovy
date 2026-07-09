package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticCorpusRequestDTO
import org.maurodata.domain.search.dto.SemanticIndexJobDTO
import org.maurodata.domain.search.dto.SemanticIndexRebuildResponseDTO
import org.maurodata.domain.search.dto.SemanticIndexingStatusDTO
import org.maurodata.domain.search.dto.SemanticModelIndexDTO
import org.maurodata.domain.search.dto.SemanticModelIndexJobStartRequestDTO
import org.maurodata.domain.search.dto.SemanticModelIndexOperationResponseDTO
import org.maurodata.domain.search.dto.SemanticModelIndexRequestDTO
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticIndexAdministrationService)
class NoOpSemanticIndexAdministrationService implements SemanticIndexAdministrationService {

    static final String REASON = 'semantic index implementation is not installed'

    @Override
    SemanticIndexRebuildResponseDTO rebuildCatalogueIndex(String indexName,
                                                          String corpusName,
                                                          List<String> domainTypes,
                                                          UUID mauroModelId,
                                                          Integer maxRows,
                                                          Integer batchSize,
                                                          boolean force) {
        SemanticIndexRebuildResponseDTO.fromMap([
            indexName: indexName ?: 'catalogue-items-default',
            corpusName: corpusName ?: 'catalogue-items',
            status: 'unavailable',
            reason: REASON
        ] as Map<String, Object>)
    }

    @Override
    List<SemanticIndexJobDTO> reconcileDeclaredIndexes() {
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
    SemanticIndexingStatusDTO indexingStatus() {
        SemanticIndexingStatusDTO.fromMap([enabled: false, autoReconcile: false, reason: REASON] as Map<String, Object>)
    }

    @Override
    SemanticIndexingStatusDTO setIndexingEnabled(boolean enabled) {
        SemanticIndexingStatusDTO.fromMap([enabled: false, requestedEnabled: enabled, reason: REASON] as Map<String, Object>)
    }

    @Override
    boolean autoReconcileEnabled() {
        false
    }

    @Override
    SemanticIndexingStatusDTO setAutoReconcileEnabled(boolean enabled) {
        SemanticIndexingStatusDTO.fromMap([enabled: false, autoReconcile: false, requestedAutoReconcile: enabled, reason: REASON] as Map<String, Object>)
    }

    @Override
    List<SemanticCorpusDTO> corpora() {
        Collections.emptyList()
    }

    @Override
    SemanticCorpusDTO createCorpus(SemanticCorpusRequestDTO request) {
        SemanticCorpusDTO.fromMap([name: request?.name, enabled: false, description: REASON] as Map<String, Object>)
    }

    @Override
    List<SemanticModelIndexDTO> modelIndexes() {
        Collections.emptyList()
    }

    @Override
    List<SemanticModelIndexDTO> modelIndexStats(UUID mauroModelId) {
        Collections.emptyList()
    }

    @Override
    SemanticModelIndexDTO createModelIndex(SemanticModelIndexRequestDTO request) {
        SemanticModelIndexDTO.fromMap([mauroModelId: request?.mauroModelId ?: request?.modelId, profileName: request?.profileName, corpusName: request?.corpusName, enabled: false, status: 'unavailable', lastError: REASON] as Map<String, Object>)
    }

    @Override
    SemanticModelIndexOperationResponseDTO deleteModelIndex(UUID mauroModelId, String profileName, String corpusName, boolean deleteEmbeddings) {
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName ?: 'catalogue-items',
            deleted: 0,
            deleteEmbeddings: deleteEmbeddings,
            reason: REASON
        ] as Map<String, Object>)
    }

    @Override
    SemanticModelIndexOperationResponseDTO deleteModelIndexEmbeddings(UUID mauroModelId, String profileName, String corpusName) {
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName ?: 'catalogue-items',
            deletedEmbeddings: 0,
            reason: REASON
        ] as Map<String, Object>)
    }

    @Override
    SemanticModelIndexOperationResponseDTO startModelIndexJobs(UUID mauroModelId, String profileName, String corpusName, SemanticModelIndexJobStartRequestDTO request) {
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName,
            status: 'unavailable',
            reason: REASON
        ] as Map<String, Object>)
    }

    @Override
    List<SemanticIndexJobDTO> jobs(boolean includeHistory) {
        Collections.emptyList()
    }

    @Override
    SemanticIndexJobDTO jobStatus(UUID jobId) {
        SemanticIndexJobDTO.fromMap([jobId: jobId?.toString(), status: 'unavailable', error: REASON] as Map<String, Object>)
    }

    @Override
    SemanticIndexJobDTO cancelJob(UUID jobId) {
        SemanticIndexJobDTO.fromMap([jobId: jobId?.toString(), status: 'unavailable', error: REASON] as Map<String, Object>)
    }

    @Override
    SemanticIndexJobDTO resumeJob(UUID jobId) {
        SemanticIndexJobDTO.fromMap([jobId: jobId?.toString(), status: 'unavailable', error: REASON] as Map<String, Object>)
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
    List<SemanticIndexJobDTO> recoverInterruptedJobs() {
        Collections.emptyList()
    }
}
