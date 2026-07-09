package org.maurodata.service.semantic

import groovy.transform.CompileStatic
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

@CompileStatic
interface SemanticIndexAdministrationService {

    SemanticIndexRebuildResponseDTO rebuildCatalogueIndex(String indexName,
                                                          String corpusName,
                                                          List<String> domainTypes,
                                                          UUID mauroModelId,
                                                          Integer maxRows,
                                                          Integer batchSize,
                                                          boolean force)

    List<SemanticIndexJobDTO> reconcileDeclaredIndexes()

    boolean hasEmbeddings(String indexName)

    boolean hasEmbeddings(String indexName, UUID mauroModelId)

    SemanticIndexingStatusDTO indexingStatus()

    SemanticIndexingStatusDTO setIndexingEnabled(boolean enabled)

    boolean autoReconcileEnabled()

    SemanticIndexingStatusDTO setAutoReconcileEnabled(boolean enabled)

    List<SemanticCorpusDTO> corpora()

    SemanticCorpusDTO createCorpus(SemanticCorpusRequestDTO request)

    List<SemanticModelIndexDTO> modelIndexes()

    List<SemanticModelIndexDTO> modelIndexStats(UUID mauroModelId)

    SemanticModelIndexDTO createModelIndex(SemanticModelIndexRequestDTO request)

    SemanticModelIndexOperationResponseDTO deleteModelIndex(UUID mauroModelId, String profileName, String corpusName, boolean deleteEmbeddings)

    SemanticModelIndexOperationResponseDTO deleteModelIndexEmbeddings(UUID mauroModelId, String profileName, String corpusName)

    SemanticModelIndexOperationResponseDTO startModelIndexJobs(UUID mauroModelId, String profileName, String corpusName, SemanticModelIndexJobStartRequestDTO request)

    List<SemanticIndexJobDTO> jobs(boolean includeHistory)

    SemanticIndexJobDTO jobStatus(UUID jobId)

    SemanticIndexJobDTO cancelJob(UUID jobId)

    SemanticIndexJobDTO resumeJob(UUID jobId)

    String jobEvents(UUID jobId)

    Publisher<String> followJobEvents(UUID jobId, Long afterSequence)

    List<SemanticIndexJobDTO> recoverInterruptedJobs()
}
