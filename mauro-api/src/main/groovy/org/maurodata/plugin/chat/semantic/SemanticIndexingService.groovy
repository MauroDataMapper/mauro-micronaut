package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticCorpusRequestDTO
import org.maurodata.domain.search.dto.SemanticIndexJobDTO
import org.maurodata.domain.search.dto.SemanticIndexRebuildResponseDTO
import org.maurodata.domain.search.dto.SemanticIndexingStatusDTO
import org.maurodata.domain.search.dto.SemanticModelIndexDTO
import org.maurodata.domain.search.dto.SemanticModelIndexJobStartRequestDTO
import org.maurodata.domain.search.dto.SemanticModelIndexOperationResponseDTO
import org.maurodata.domain.search.dto.SemanticModelIndexRequestDTO

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.data.connection.annotation.Connectable
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.time.Duration
import java.util.function.Consumer

@Slf4j
@CompileStatic
@Singleton
class SemanticIndexingService implements SemanticIndexAdministrationService {

    private final SemanticRepository semanticRepository
    private final EmbeddingProviderRegistry embeddingProviderRegistry
    private final int embeddingBatchSize
    private final int deferVectorIndexThreshold
    private final boolean deferVectorIndexWithExistingEmbeddings
    private final boolean rebuildVectorIndexAfterPartialLoad
    private final boolean reuseDuplicateContentBeforeEmbedding
    private final int minimumAdaptiveBatchSize
    private final long progressLogIntervalMillis
    private final String defaultEmbeddingProfile
    private final ExecutorService executorService

    SemanticIndexingService(SemanticRepository semanticRepository,
                            EmbeddingProviderRegistry embeddingProviderRegistry,
                            @Value('${chat.semantic.embeddings.batch-size:512}') Integer embeddingBatchSize,
                            @Value('${chat.semantic.embeddings.defer-vector-index-threshold:10000}') Integer deferVectorIndexThreshold,
                            @Value('${chat.semantic.embeddings.defer-vector-index-with-existing-embeddings:false}') Boolean deferVectorIndexWithExistingEmbeddings,
                            @Value('${chat.semantic.embeddings.rebuild-vector-index-after-partial-load:false}') Boolean rebuildVectorIndexAfterPartialLoad,
                            @Value('${chat.semantic.embeddings.reuse-duplicate-content-before-embedding:false}') Boolean reuseDuplicateContentBeforeEmbedding,
                            @Value('${chat.semantic.embeddings.adaptive-min-batch-size:128}') Integer minimumAdaptiveBatchSize,
                            @Value('${chat.semantic.embeddings.progress-log-interval-seconds:30}') Integer progressLogIntervalSeconds,
                            @Value('${chat.semantic.default-embedding-profile:ollama-nomic-embed-text}') String defaultEmbeddingProfile,
                            @Value('${chat.semantic.indexing.worker-threads:1}') Integer workerThreads) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
        this.executorService = Executors.newFixedThreadPool(
            Math.max(workerThreads ?: 1, 1),
            semanticIndexThreadFactory()
        )
        this.embeddingBatchSize = Math.max(embeddingBatchSize ?: 32, 1)
        this.deferVectorIndexThreshold = Math.max(deferVectorIndexThreshold ?: 0, 0)
        this.deferVectorIndexWithExistingEmbeddings = deferVectorIndexWithExistingEmbeddings == true
        this.rebuildVectorIndexAfterPartialLoad = rebuildVectorIndexAfterPartialLoad == true
        this.reuseDuplicateContentBeforeEmbedding = reuseDuplicateContentBeforeEmbedding == true
        this.minimumAdaptiveBatchSize = Math.max(minimumAdaptiveBatchSize ?: 128, 1)
        this.progressLogIntervalMillis = Math.max(progressLogIntervalSeconds ?: 30, 1) * 1000L
        this.defaultEmbeddingProfile = defaultEmbeddingProfile ?: 'ollama-nomic-embed-text'
    }

    @PreDestroy
    void shutdownExecutor() {
        try {
            List<Map<String, Object>> cancelledJobs = semanticRepository.cancelActiveJobs('application is shutting down; semantic indexing job will not be auto-recovered')
            if (!cancelledJobs.isEmpty()) {
                log.info('Cancelled {} active semantic indexing jobs during shutdown', Integer.valueOf(cancelledJobs.size()))
            }
        } catch (Throwable t) {
            log.warn('Failed to mark active semantic indexing jobs as cancelled during shutdown', t)
        }
        executorService.shutdownNow()
        try {
            if (!executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                log.warn('Semantic indexing executor did not terminate cleanly within shutdown timeout')
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt()
        }
    }

    private static ThreadFactory semanticIndexThreadFactory() {
        AtomicInteger threadNumber = new AtomicInteger(1)
        return {Runnable runnable ->
            Thread thread = new Thread(runnable, "semantic-indexing-worker-${threadNumber.getAndIncrement()}".toString())
            thread.daemon = true
            thread
        } as ThreadFactory
    }

    @Connectable
    @Override
    SemanticIndexRebuildResponseDTO rebuildCatalogueIndex(String indexName = 'catalogue-items-default',
                                                          String corpusName = 'catalogue-items',
                                                          List<String> domainTypes = [],
                                                          UUID mauroModelId = null,
                                                          Integer maxRows = null,
                                                          Integer batchSize = null,
                                                          boolean force = false) {
        long start = System.currentTimeMillis()
        int effectiveBatchSize = Math.max(batchSize ?: embeddingBatchSize, 1)
        List<EmbeddingProfile> profiles = semanticRepository.profilesForIndex(indexName)
        if (profiles.isEmpty()) {
            throw new IllegalStateException("No embedding profiles configured for semantic index ${indexName}")
        }
        SemanticIndexRebuildResponseDTO.fromMap(rebuildCatalogueIndexWithProfiles(profiles, indexName, corpusName, domainTypes, mauroModelId, maxRows, batchSize, force))
    }

    private Map<String, Object> rebuildCatalogueIndexWithProfiles(List<EmbeddingProfile> profiles,
                                                                  String indexName = 'catalogue-items-default',
                                                                  String corpusName = 'catalogue-items',
                                                                  List<String> domainTypes = [],
                                                                  UUID mauroModelId = null,
                                                                  Integer maxRows = null,
                                                                  Integer batchSize = null,
                                                                  boolean force = false,
                                                                  Closure<Boolean> cancellationRequested = null,
                                                                  Consumer<Map<String, Object>> progressConsumer = null) {
        long start = System.currentTimeMillis()
        int effectiveBatchSize = Math.max(batchSize ?: embeddingBatchSize, 1)
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalStateException("No embedding profiles configured for semantic index ${indexName}")
        }

        List<String> profileNames = profiles.collect {EmbeddingProfile profile -> profile.name} as List<String>
        emitProgress(progressConsumer, baseProgress(
            'starting',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        ))
        emitProgress(progressConsumer, baseProgress(
            'refreshing_context',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        ))
        boolean contextRefreshed = semanticRepository.refreshAdministeredItemContextIfExists()
        Map<String, Object> contextProgress = baseProgress(
            'context_refreshed',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        contextProgress.put('administeredItemContextRefreshed', contextRefreshed)
        emitProgress(progressConsumer, contextProgress)
        semanticRepository.updateIndexStatus(indexName, 'INDEXING')
        log.info(
            'Semantic index rebuild starting indexName={} corpusName={} profiles={} domainTypes={} mauroModelId={} maxRows={} batchSize={} force={} administeredItemContextRefreshed={}',
            indexName,
            corpusName,
            profileNames,
            domainTypes ?: [],
            mauroModelId,
            maxRows,
            effectiveBatchSize,
            Boolean.valueOf(force),
            Boolean.valueOf(contextRefreshed)
        )

        emitProgress(progressConsumer, baseProgress(
            'counting_candidate_chunks',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        ))
        int chunkCount = semanticRepository.countCatalogueCandidateChunks(corpusName, domainTypes, mauroModelId, maxRows)
        log.info('Semantic index rebuild selected {} candidate chunks for indexName={}', Integer.valueOf(chunkCount), indexName)
        Map<String, Object> countedProgress = baseProgress(
            'candidate_chunks_counted',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        countedProgress.put('chunks', chunkCount)
        emitProgress(progressConsumer, countedProgress)
        Map<String, Object> reconcileProgress = baseProgress(
            'reconciling_chunks',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        reconcileProgress.put('chunks', chunkCount)
        emitProgress(progressConsumer, reconcileProgress)
        Map<String, Integer> chunkReconcileResult = semanticRepository.reconcileCatalogueChunksDetailed(corpusName, domainTypes, mauroModelId, maxRows)
        int upsertedChunkCount = chunkReconcileResult.get('upsertedChunks') ?: 0
        int deletedChunkCount = chunkReconcileResult.get('deletedChunks') ?: 0
        int changedChunkCount = chunkReconcileResult.get('changedChunks') ?: 0
        Map<String, Object> reconciledProgress = baseProgress(
            'chunks_reconciled',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        reconciledProgress.put('chunks', chunkCount)
        reconciledProgress.put('changedChunks', changedChunkCount)
        reconciledProgress.put('upsertedChunks', upsertedChunkCount)
        reconciledProgress.put('deletedChunks', deletedChunkCount)
        emitProgress(progressConsumer, reconciledProgress)
        Map<String, Object> syncProgress = baseProgress(
            'syncing_embedding_chunk_groups',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        syncProgress.put('chunks', chunkCount)
        syncProgress.put('changedChunks', changedChunkCount)
        syncProgress.put('upsertedChunks', upsertedChunkCount)
        syncProgress.put('deletedChunks', deletedChunkCount)
        emitProgress(progressConsumer, syncProgress)
        int syncedEmbeddingGroups = semanticRepository.syncEmbeddingChunkGroups(corpusName, domainTypes, mauroModelId, maxRows)
        int embeddingCount = 0
        int skippedEmbeddingCount = 0
        int reusedEmbeddingCount = 0
        Map<String, Integer> profileEmbeddingCounts = new LinkedHashMap<String, Integer>()
        Map<String, Integer> profileSkippedCounts = new LinkedHashMap<String, Integer>()
        Map<String, Integer> profileReusedCounts = new LinkedHashMap<String, Integer>()
        log.info(
            'Semantic index rebuild reconciled chunk records for indexName={} candidateChunks={} changedChunkRows={} upsertedChunkRows={} deletedChunkRows={} syncedEmbeddingChunkGroups={}',
            indexName,
            Integer.valueOf(chunkCount),
            Integer.valueOf(changedChunkCount),
            Integer.valueOf(upsertedChunkCount),
            Integer.valueOf(deletedChunkCount),
            Integer.valueOf(syncedEmbeddingGroups)
        )
        Map<String, Object> syncedProgress = baseProgress(
            'embedding_chunk_groups_synced',
            start,
            indexName,
            corpusName,
            mauroModelId,
            profileNames,
            effectiveBatchSize,
            force
        )
        syncedProgress.put('chunks', chunkCount)
        syncedProgress.put('changedChunks', changedChunkCount)
        syncedProgress.put('upsertedChunks', upsertedChunkCount)
        syncedProgress.put('deletedChunks', deletedChunkCount)
        syncedProgress.put('syncedEmbeddingChunkGroups', syncedEmbeddingGroups)
        emitProgress(progressConsumer, syncedProgress)

        try {
            for (EmbeddingProfile profile : profiles) {
                Map<String, Object> preparingProfileProgress = baseProgress(
                    'preparing_embedding_profile',
                    start,
                    indexName,
                    corpusName,
                    mauroModelId,
                    profileNames,
                    effectiveBatchSize,
                    force
                )
                preparingProfileProgress.put('profileName', profile.name)
                preparingProfileProgress.put('chunks', chunkCount)
                emitProgress(progressConsumer, preparingProfileProgress)
                EmbeddingProvider provider = embeddingProviderRegistry.providerFor(profile)
                log.info('Semantic index profile {} preparing embeddings: deleting stale rows', profile.name)
                List<Map<String, Object>> staleCounts = semanticRepository.staleEmbeddingCountsByChunkKind(profile, 12)
                if (!staleCounts.isEmpty()) {
                    log.warn('Semantic index profile {} stale embedding breakdown before delete: {}', profile.name, staleCounts)
                }
                int staleEmbeddings = semanticRepository.deleteStaleEmbeddings(profile)
                log.info('Semantic index profile {} deleted {} stale embeddings', profile.name, Integer.valueOf(staleEmbeddings))
                if (force) {
                    log.info('Semantic index profile {} force rebuild requested; skipping duplicate-content embedding reuse', profile.name)
                } else if (!reuseDuplicateContentBeforeEmbedding) {
                    log.info('Semantic index profile {} duplicate-content embedding reuse disabled; skipping pre-embedding reuse pass', profile.name)
                } else {
                    log.info('Semantic index profile {} reusing duplicate-content embeddings where available', profile.name)
                }
                int reusedForProfile = force || !reuseDuplicateContentBeforeEmbedding ? 0 : semanticRepository.reuseEmbeddingsForMatchingContentHashes(profile, corpusName, domainTypes, mauroModelId, maxRows)
                if (reuseDuplicateContentBeforeEmbedding && !force) {
                    log.info('Semantic index profile {} reused {} duplicate-content embeddings', profile.name, Integer.valueOf(reusedForProfile))
                }
                log.info('Semantic index profile {} counting chunks still needing embeddings', profile.name)
                reusedEmbeddingCount += reusedForProfile
                int chunksToEmbedCount = semanticRepository.countChunksNeedingEmbedding(profile, corpusName, domainTypes, mauroModelId, maxRows, force)
                log.info('Semantic index profile {} counted {} chunks still needing embeddings', profile.name, Integer.valueOf(chunksToEmbedCount))
                int skippedForProfile = chunkCount - chunksToEmbedCount
                skippedEmbeddingCount += skippedForProfile
                profileSkippedCounts.put(profile.name, skippedForProfile)
                profileReusedCounts.put(profile.name, reusedForProfile)
                profileEmbeddingCounts.put(profile.name, 0)
                log.info(
                    'Semantic index profile {} has {} chunks needing embeddings, {} unchanged/reused chunks skipped, {} duplicate-content embeddings reused, {} stale embeddings removed',
                    profile.name,
                    Integer.valueOf(chunksToEmbedCount),
                    Integer.valueOf(skippedForProfile),
                    Integer.valueOf(reusedForProfile),
                    Integer.valueOf(staleEmbeddings)
                )
                logChunkSamples(profile, corpusName, domainTypes, mauroModelId, maxRows, force)

                long existingEmbeddingCount = semanticRepository.countEmbeddingsForProfile(profile)
                boolean vectorIndexExists = semanticRepository.vectorIndexExists(profile)
                if (existingEmbeddingCount > 0L && !vectorIndexExists) {
                    log.warn(
                        'Semantic index profile {} has {} existing embeddings but no HNSW index; rebuilding HNSW before continuing',
                        profile.name,
                        Long.valueOf(existingEmbeddingCount)
                    )
                    semanticRepository.createVectorIndex(profile)
                    vectorIndexExists = true
                }

                boolean deferVectorIndex = deferVectorIndexThreshold > 0 &&
                    chunksToEmbedCount >= deferVectorIndexThreshold &&
                    (existingEmbeddingCount == 0L || deferVectorIndexWithExistingEmbeddings)
                if (deferVectorIndex) {
                    log.info(
                        'Semantic index profile {} deferring HNSW index maintenance while inserting {} embeddings (existingEmbeddings={}, threshold={}, deferWithExisting={})',
                        profile.name,
                        Integer.valueOf(chunksToEmbedCount),
                        Long.valueOf(existingEmbeddingCount),
                        Integer.valueOf(deferVectorIndexThreshold),
                        Boolean.valueOf(deferVectorIndexWithExistingEmbeddings)
                    )
                    semanticRepository.dropVectorIndex(profile)
                } else if (chunksToEmbedCount >= deferVectorIndexThreshold && existingEmbeddingCount > 0L) {
                    log.info(
                        'Semantic index profile {} keeping existing HNSW index live while inserting {} embeddings because {} embeddings are already searchable',
                        profile.name,
                        Integer.valueOf(chunksToEmbedCount),
                        Long.valueOf(existingEmbeddingCount)
                    )
                }

                int embeddedForProfile = 0
                int batchNumber = 0
                int totalBatches = chunksToEmbedCount == 0 ? 0 : ((chunksToEmbedCount + effectiveBatchSize - 1).intdiv(effectiveBatchSize))
                long profileEmbeddingStart = System.currentTimeMillis()
                long lastProgressLog = 0L
                long chunkFetchMillis = 0L
                long providerEmbedMillis = 0L
                long embeddingUpsertMillis = 0L
                long lastLoggedFetchMillis = 0L
                long lastLoggedEmbedMillis = 0L
                long lastLoggedUpsertMillis = 0L
                UUID lastProcessedChunkId = null
                boolean completedEmbeddingLoad = false
                Throwable embeddingLoadFailure = null
                emitProgress(progressConsumer, [
                    stage: 'embedding_profile_started',
                    indexName: indexName,
                    corpusName: corpusName,
                    mauroModelId: mauroModelId?.toString(),
                    profileName: profile.name,
                    chunks: chunkCount,
                    chunksToEmbed: chunksToEmbedCount,
                    embeddedForProfile: embeddedForProfile,
                    skippedForProfile: skippedForProfile,
                    reusedForProfile: reusedForProfile,
                    staleDeletedForProfile: staleEmbeddings,
                    batchSize: effectiveBatchSize,
                    batchNumber: batchNumber,
                    totalBatches: totalBatches,
                    rebuildEmbeddings: force,
                    elapsedMs: System.currentTimeMillis() - start
                ] as Map<String, Object>)
                try {
                    for (;;) {
                        long fetchStart = System.currentTimeMillis()
                        List<SemanticChunk> batch = semanticRepository.nextChunksNeedingEmbedding(
                            profile,
                            corpusName,
                            domainTypes,
                            mauroModelId,
                            maxRows,
                            force,
                            effectiveBatchSize,
                            lastProcessedChunkId
                        )
                        chunkFetchMillis += System.currentTimeMillis() - fetchStart
                        if (batch.isEmpty()) {
                            break
                        }
                        if (cancellationRequested != null && Boolean.TRUE.equals(cancellationRequested.call())) {
                            throw new CancellationException('semantic indexing job was cancelled')
                        }
                        batchNumber++
                        BatchEmbeddingResult batchEmbeddingResult = embedAndUpsertAdaptively(provider, profile, batch, batch.size())
                        lastProcessedChunkId = batch.get(batch.size() - 1).id
                        providerEmbedMillis += batchEmbeddingResult.embeddingMillis
                        embeddingUpsertMillis += batchEmbeddingResult.upsertMillis
                        embeddingCount += batch.size()
                        embeddedForProfile += batch.size()
                        profileEmbeddingCounts.put(profile.name, embeddedForProfile)
                        long now = System.currentTimeMillis()
                        if (shouldLogEmbeddingProgress(batchNumber, totalBatches, now, lastProgressLog)) {
                            long windowFetchMillis = chunkFetchMillis - lastLoggedFetchMillis
                            long windowEmbedMillis = providerEmbedMillis - lastLoggedEmbedMillis
                            long windowUpsertMillis = embeddingUpsertMillis - lastLoggedUpsertMillis
                            lastProgressLog = now
                            lastLoggedFetchMillis = chunkFetchMillis
                            lastLoggedEmbedMillis = providerEmbedMillis
                            lastLoggedUpsertMillis = embeddingUpsertMillis
                            long elapsedMillis = Math.max(now - profileEmbeddingStart, 1L)
                            double chunksPerSecond = (embeddedForProfile * 1000.0D) / elapsedMillis
                            int remainingChunks = Math.max(chunksToEmbedCount - embeddedForProfile, 0)
                            long etaMillis = chunksPerSecond > 0.0D ? Math.round((remainingChunks * 1000.0D) / chunksPerSecond) : 0L
                            emitProgress(progressConsumer, [
                                stage: 'embedding_profile_running',
                                indexName: indexName,
                                corpusName: corpusName,
                                mauroModelId: mauroModelId?.toString(),
                                profileName: profile.name,
                                chunks: chunkCount,
                                chunksToEmbed: chunksToEmbedCount,
                                embeddedForProfile: embeddedForProfile,
                                totalEmbedded: embeddingCount,
                                skippedForProfile: skippedForProfile,
                                reusedForProfile: reusedForProfile,
                                batchNumber: batchNumber,
                                totalBatches: totalBatches,
                                remainingChunks: remainingChunks,
                                chunksPerSecond: Math.round(chunksPerSecond * 10D) / 10D,
                                etaMs: etaMillis,
                                elapsedMs: System.currentTimeMillis() - start,
                                fetchMs: chunkFetchMillis,
                                embeddingMs: providerEmbedMillis,
                                upsertMs: embeddingUpsertMillis
                            ] as Map<String, Object>)
                            log.info(
                                'Semantic index profile {} progress: embedded {}/{} chunks (batch {}/{}), elapsed={}, rate={} chunks/s, eta={}, cumulative(fetch={}, embed={}, upsert={}), window(fetch={}, embed={}, upsert={})',
                                profile.name,
                                Integer.valueOf(embeddedForProfile),
                                Integer.valueOf(chunksToEmbedCount),
                                Integer.valueOf(batchNumber),
                                Integer.valueOf(totalBatches),
                                formatDuration(elapsedMillis),
                                String.format('%.1f', Double.valueOf(chunksPerSecond)),
                                formatDuration(etaMillis),
                                formatDuration(chunkFetchMillis),
                                formatDuration(providerEmbedMillis),
                                formatDuration(embeddingUpsertMillis),
                                formatDuration(windowFetchMillis),
                                formatDuration(windowEmbedMillis),
                                formatDuration(windowUpsertMillis)
                            )
                        }
                    }
                    completedEmbeddingLoad = embeddedForProfile >= chunksToEmbedCount
                    emitProgress(progressConsumer, [
                        stage: 'embedding_profile_completed',
                        indexName: indexName,
                        corpusName: corpusName,
                        mauroModelId: mauroModelId?.toString(),
                        profileName: profile.name,
                        chunks: chunkCount,
                        chunksToEmbed: chunksToEmbedCount,
                        embeddedForProfile: embeddedForProfile,
                        totalEmbedded: embeddingCount,
                        skippedForProfile: skippedForProfile,
                        reusedForProfile: reusedForProfile,
                        batchNumber: batchNumber,
                        totalBatches: totalBatches,
                        elapsedMs: System.currentTimeMillis() - start
                    ] as Map<String, Object>)
                } catch (Throwable t) {
                    embeddingLoadFailure = t
                    log.error(
                        'Semantic index profile {} embedding load failed after embedding {}/{} chunks (batch {}/{}); rebuilding HNSW index cleanup will run if deferred',
                        profile.name,
                        Integer.valueOf(embeddedForProfile),
                        Integer.valueOf(chunksToEmbedCount),
                        Integer.valueOf(batchNumber),
                        Integer.valueOf(totalBatches),
                        t
                    )
                    throw t
                } finally {
                    if (deferVectorIndex) {
                        if (completedEmbeddingLoad) {
                            log.info(
                                'Semantic index profile {} rebuilding HNSW index after completed embedding load ({}/{} chunks embedded)',
                                profile.name,
                                Integer.valueOf(embeddedForProfile),
                                Integer.valueOf(chunksToEmbedCount)
                            )
                            semanticRepository.createVectorIndex(profile)
                        } else {
                            log.warn(
                                'Semantic index profile {} ended after partial embedding load ({}/{} chunks embedded); cause={}',
                                profile.name,
                                Integer.valueOf(embeddedForProfile),
                                Integer.valueOf(chunksToEmbedCount),
                                embeddingLoadFailure == null ? 'unknown' : embeddingLoadFailure.getClass().getName() + ': ' + embeddingLoadFailure.message
                            )
                            if (rebuildVectorIndexAfterPartialLoad) {
                                log.warn('Semantic index profile {} rebuilding HNSW index after partial embedding load because rebuild-vector-index-after-partial-load=true', profile.name)
                                semanticRepository.createVectorIndex(profile)
                            } else {
                                log.warn('Semantic index profile {} leaving HNSW index absent after partial embedding load; rerun indexing to resume and rebuild on completion', profile.name)
                            }
                        }
                    }
                }
            }
            semanticRepository.updateIndexStatus(indexName, 'READY')
        } catch (Throwable t) {
            semanticRepository.updateIndexStatus(indexName, 'FAILED')
            throw t
        }

        Map<String, Object> result = [
            indexName: indexName,
            corpusName: corpusName,
            profiles: profiles.collect {EmbeddingProfile profile -> profile.name},
            chunks: chunkCount,
            embeddings: embeddingCount,
            skippedEmbeddings: skippedEmbeddingCount,
            reusedEmbeddings: reusedEmbeddingCount,
            syncedEmbeddingChunkGroups: syncedEmbeddingGroups,
            profileEmbeddings: profileEmbeddingCounts,
            profileSkippedEmbeddings: profileSkippedCounts,
            profileReusedEmbeddings: profileReusedCounts,
            batchSize: effectiveBatchSize,
            force: force,
            elapsedMs: System.currentTimeMillis() - start
        ] as Map<String, Object>
        log.info('Semantic index rebuild completed: {}', result)
        result
    }

    private BatchEmbeddingResult embedAndUpsertAdaptively(EmbeddingProvider provider,
                                                          EmbeddingProfile profile,
                                                          List<SemanticChunk> batch,
                                                          int originalBatchSize) {
        if (batch == null || batch.isEmpty()) {
            return new BatchEmbeddingResult()
        }
        long embedStart = System.currentTimeMillis()
        try {
            List<String> texts = batch.collect {SemanticChunk chunk -> chunk.sourceText} as List<String>
            List<float[]> embeddings = provider.embed(profile, texts)
            long embeddingMillis = System.currentTimeMillis() - embedStart
            if (embeddings.size() != batch.size()) {
                throw new IllegalStateException("Embedding provider ${profile.provider}/${profile.embeddingModel} returned ${embeddings.size()} embeddings for ${batch.size()} chunks")
            }
            long upsertStart = System.currentTimeMillis()
            semanticRepository.upsertEmbeddings(batch, profile, embeddings)
            return new BatchEmbeddingResult(
                embeddingMillis: embeddingMillis,
                upsertMillis: System.currentTimeMillis() - upsertStart
            )
        } catch (Throwable t) {
            long failedEmbeddingMillis = System.currentTimeMillis() - embedStart
            if (batch.size() <= minimumAdaptiveBatchSize) {
                log.error(
                    'Semantic index profile {} embedding failed at minimum adaptive batch size {} after {}. Giving up on this batch.',
                    profile.name,
                    Integer.valueOf(batch.size()),
                    formatDuration(failedEmbeddingMillis),
                    t
                )
                throw t
            }
            int halfBatchSize = batch.size().intdiv(2) as int
            int splitSize = halfBatchSize > minimumAdaptiveBatchSize ? halfBatchSize : minimumAdaptiveBatchSize
            log.warn(
                'Semantic index profile {} embedding batch of {} chunks failed after {}; retrying as sub-batches of up to {} chunks (original configured batch size={}); cause={}: {}',
                profile.name,
                Integer.valueOf(batch.size()),
                formatDuration(failedEmbeddingMillis),
                Integer.valueOf(splitSize),
                Integer.valueOf(originalBatchSize),
                t.getClass().getName(),
                t.message
            )
            BatchEmbeddingResult total = new BatchEmbeddingResult(embeddingMillis: failedEmbeddingMillis)
            for (List<SemanticChunk> subBatch : batch.collate(splitSize)) {
                BatchEmbeddingResult subResult = embedAndUpsertAdaptively(provider, profile, subBatch, originalBatchSize)
                total.embeddingMillis += subResult.embeddingMillis
                total.upsertMillis += subResult.upsertMillis
            }
            return total
        }
    }

    @CompileStatic
    private static class BatchEmbeddingResult {
        long embeddingMillis = 0L
        long upsertMillis = 0L
    }

    private boolean shouldLogEmbeddingProgress(int batchNumber, int totalBatches, long now, long lastProgressLog) {
        batchNumber == 1 ||
            batchNumber == totalBatches ||
            batchNumber % 25 == 0 ||
            lastProgressLog == 0L ||
            now - lastProgressLog >= progressLogIntervalMillis
    }

    private static void emitProgress(Consumer<Map<String, Object>> progressConsumer,
                                     Map<String, Object> progress) {
        if (progressConsumer != null) {
            progressConsumer.accept(progress)
        }
    }

    private static Map<String, Object> baseProgress(String stage,
                                                    long start,
                                                    String indexName,
                                                    String corpusName,
                                                    UUID mauroModelId,
                                                    List<String> profileNames,
                                                    int batchSize,
                                                    boolean rebuildEmbeddings) {
        [
            stage: stage,
            indexName: indexName,
            corpusName: corpusName,
            mauroModelId: mauroModelId?.toString(),
            profiles: profileNames,
            batchSize: batchSize,
            rebuildEmbeddings: rebuildEmbeddings,
            elapsedMs: System.currentTimeMillis() - start
        ] as Map<String, Object>
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(Math.round(millis / 1000.0D), 0L)
        long hours = totalSeconds.intdiv(3600)
        long minutes = (totalSeconds % 3600L).intdiv(60)
        long seconds = totalSeconds % 60L
        if (hours > 0L) {
            return String.format('%dh%02dm%02ds', Long.valueOf(hours), Long.valueOf(minutes), Long.valueOf(seconds))
        }
        if (minutes > 0L) {
            return String.format('%dm%02ds', Long.valueOf(minutes), Long.valueOf(seconds))
        }
        return String.format('%ds', Long.valueOf(seconds))
    }

    private void logChunkSamples(EmbeddingProfile profile,
                                 String corpusName,
                                 List<String> domainTypes,
                                 UUID mauroModelId,
                                 Integer maxRows,
                                 boolean force) {
        List<SemanticChunk> samples = semanticRepository.nextChunksNeedingEmbedding(
            profile,
            corpusName,
            domainTypes,
            mauroModelId,
            maxRows,
            force,
            8,
            null
        )
        if (samples.isEmpty()) {
            return
        }
        log.info('Semantic index profile {} sample chunks needing embeddings:', profile.name)
        int ordinal = 1
        for (SemanticChunk chunk : samples) {
            log.info(
                '  sample {}: group={} kind={} sourceType={} domainType={} label={} text="{}"',
                Integer.valueOf(ordinal++),
                chunk.chunkGroup ?: '',
                chunk.chunkKind ?: '',
                chunk.sourceType ?: '',
                chunk.sourceDomainType ?: '',
                truncateForLog(chunk.sourceLabel, 80),
                truncateForLog(chunk.sourceText, 220)
            )
        }
    }

    private static String truncateForLog(String value, int maxLength) {
        if (value == null) {
            return ''
        }
        String normalized = value.replaceAll('\\s+', ' ').trim()
        if (normalized.size() <= maxLength) {
            return normalized
        }
        normalized.substring(0, Math.max(maxLength - 3, 0)) + '...'
    }

    @Connectable
    @Override
    List<SemanticIndexJobDTO> reconcileDeclaredIndexes() {
        if (!semanticRepository.indexingEnabled()) {
            log.info('Semantic index reconcile skipped because indexing is disabled')
            return Collections.singletonList(SemanticIndexJobDTO.fromMap([status: 'skipped', result: [enabled: false, reason: 'semantic indexing is disabled']] as Map<String, Object>))
        }
        List<Map<String, Object>> indexes = semanticRepository.modelIndexes().findAll {Map<String, Object> index ->
            Boolean.TRUE.equals(index.get('enabled'))
        } as List<Map<String, Object>>
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> index : indexes) {
            UUID mauroModelId = UUID.fromString(String.valueOf(index.get('mauroModelId')))
            String profileName = String.valueOf(index.get('profileName'))
            String corpusName = String.valueOf(index.get('corpusName') ?: 'catalogue-items')
            boolean alreadyStale = String.valueOf(index.get('status')) != 'READY'
            if (!alreadyStale) {
                continue
            }
            semanticRepository.markModelIndexStale(mauroModelId, profileName, corpusName, 'declared semantic model index changed')
            results.add(queueModelIndexJob(
                mauroModelId,
                profileName,
                corpusName,
                null,
                null,
                false,
                [
                    accepted: true,
                    source: 'auto-reconcile',
                    staleDetected: true
                ] as Map<String, Object>
            ))
        }
        SemanticIndexJobDTO.listFrom(results)
    }

    @Connectable
    @Override
    boolean hasEmbeddings(String indexName = 'catalogue-items-default') {
        semanticRepository.hasEmbeddings(indexName)
    }

    @Connectable
    @Override
    boolean hasEmbeddings(String indexName = 'catalogue-items-default', UUID mauroModelId) {
        if (mauroModelId == null) {
            return semanticRepository.hasEmbeddings(indexName)
        }
        semanticRepository.modelIndexStats(mauroModelId).any {Map<String, Object> index ->
            index.get('mauroModelId') == mauroModelId.toString() &&
                Boolean.TRUE.equals(index.get('enabled')) &&
                index.get('status') == 'READY' &&
                ((Number) index.get('embeddings')).longValue() > 0L
        }
    }

    @Connectable
    @Override
    SemanticIndexingStatusDTO indexingStatus() {
        SemanticIndexingStatusDTO.fromMap(semanticRepository.indexingStatus())
    }

    @Connectable
    @Override
    SemanticIndexingStatusDTO setIndexingEnabled(boolean enabled) {
        Map<String, Object> status = semanticRepository.setIndexingEnabled(enabled)
        if (!enabled) {
            status.put('cancelledJobs', semanticRepository.cancelActiveJobs('semantic indexing was disabled'))
        } else if (semanticRepository.autoReconcileEnabled()) {
            status.put('reconcileResults', reconcileDeclaredIndexes())
        }
        SemanticIndexingStatusDTO.fromMap(status)
    }

    @Connectable
    @Override
    boolean autoReconcileEnabled() {
        semanticRepository.autoReconcileEnabled()
    }

    @Connectable
    @Override
    SemanticIndexingStatusDTO setAutoReconcileEnabled(boolean enabled) {
        Map<String, Object> status = semanticRepository.setAutoReconcileEnabled(enabled)
        if (enabled && semanticRepository.indexingEnabled()) {
            status.put('reconcileResults', reconcileDeclaredIndexes())
        }
        SemanticIndexingStatusDTO.fromMap(status)
    }

    @Connectable
    @Override
    List<SemanticModelIndexDTO> modelIndexes() {
        SemanticModelIndexDTO.listFrom(semanticRepository.modelIndexes())
    }

    @Connectable
    @Override
    List<SemanticCorpusDTO> corpora() {
        semanticRepository.corpora(false).collect {Map<String, Object> corpus -> SemanticCorpusDTO.fromMap(corpus)} as List<SemanticCorpusDTO>
    }

    @Connectable
    @Override
    SemanticCorpusDTO createCorpus(SemanticCorpusRequestDTO request) {
        SemanticCorpusDTO.fromMap(semanticRepository.createCorpus(request == null ? null : request.toMap()))
    }

    @Connectable
    @Override
    List<SemanticModelIndexDTO> modelIndexStats(UUID mauroModelId) {
        SemanticModelIndexDTO.listFrom(semanticRepository.modelIndexStats(mauroModelId))
    }

    @Connectable
    @Override
    SemanticModelIndexDTO createModelIndex(SemanticModelIndexRequestDTO request) {
        Map<String, Object> requestMap = request == null ? Collections.<String, Object>emptyMap() : request.toMap()
        UUID mauroModelId = request?.mauroModelId ?: request?.modelId ?: uuidValue(requestMap, 'mauroModelId') ?: uuidValue(requestMap, 'modelId')
        if (mauroModelId == null) {
            throw new IllegalArgumentException('Missing required field mauroModelId')
        }
        String profileName = requiredString(requestMap, 'profileName')
        String corpusName = requiredString(requestMap, 'corpusName')
        String label = stringValue(requestMap, 'label')
        boolean enabled = booleanValue(requestMap, 'enabled', true)
        Map<String, Object> declaration = semanticRepository.createModelIndex(mauroModelId, profileName, corpusName, enabled, label)
        if (enabled && semanticRepository.indexingEnabled() && semanticRepository.autoReconcileEnabled()) {
            semanticRepository.markModelIndexStale(mauroModelId, profileName, corpusName, 'enabled semantic model index declaration created while auto reconcile is active')
            declaration.put('job', queueModelIndexJob(
                mauroModelId,
                profileName,
                corpusName,
                null,
                null,
                false,
                [
                    accepted: true,
                    source: 'model-index-created',
                    staleDetected: true
                ] as Map<String, Object>
            ))
        }
        SemanticModelIndexDTO.fromMap(declaration)
    }

    @Connectable
    @Override
    SemanticModelIndexOperationResponseDTO deleteModelIndex(UUID mauroModelId, String profileName, String corpusName, boolean deleteEmbeddings) {
        List<Map<String, Object>> declarations = matchingModelIndexes(mauroModelId, profileName, corpusName)
        List<Map<String, Object>> deleted = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> declaration : declarations) {
            String declarationProfile = String.valueOf(declaration.get('profileName'))
            String declarationCorpus = String.valueOf(declaration.get('corpusName'))
            Map<String, Object> result = semanticRepository.deleteModelIndex(mauroModelId, declarationProfile, declarationCorpus)
            if (deleteEmbeddings && ((Number) result.get('deleted')).intValue() > 0) {
                result.put('embeddings', semanticRepository.deleteEmbeddingsForModelIndex(mauroModelId, declarationProfile, declarationCorpus))
            }
            deleted.add(result)
        }
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName,
            deleteEmbeddings: deleteEmbeddings,
            matched: declarations.size(),
            deleted: deleted
        ] as Map<String, Object>)
    }

    @Connectable
    @Override
    SemanticModelIndexOperationResponseDTO deleteModelIndexEmbeddings(UUID mauroModelId, String profileName, String corpusName) {
        List<Map<String, Object>> declarations = matchingModelIndexes(mauroModelId, profileName, corpusName)
        List<Map<String, Object>> deleted = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> declaration : declarations) {
            deleted.add(semanticRepository.deleteEmbeddingsForModelIndex(
                mauroModelId,
                String.valueOf(declaration.get('profileName')),
                String.valueOf(declaration.get('corpusName'))
            ))
        }
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName,
            matched: declarations.size(),
            deleted: deleted
        ] as Map<String, Object>)
    }

    @Connectable
    @Override
    SemanticModelIndexOperationResponseDTO startModelIndexJobs(UUID mauroModelId, String profileName, String corpusName, SemanticModelIndexJobStartRequestDTO request) {
        Map<String, Object> requestMap = request == null ? Collections.<String, Object>emptyMap() : request.toMap()
        boolean runWhenIndexingDisabled = booleanValue(requestMap, 'runWhenIndexingDisabled', false)
        boolean rebuildEmbeddings = booleanValue(requestMap, 'rebuildEmbeddings', false)
        if (!semanticRepository.indexingEnabled() && !runWhenIndexingDisabled) {
            return SemanticModelIndexOperationResponseDTO.fromMap([
                mauroModelId: mauroModelId?.toString(),
                profileName: profileName,
                corpusName: corpusName,
                status: 'skipped',
                reason: 'semantic indexing is disabled',
                hint: 'Call with runWhenIndexingDisabled: true to start this explicit indexing job while global indexing is disabled.'
            ] as Map<String, Object>)
        }
        List<Map<String, Object>> declarations = matchingModelIndexes(mauroModelId, profileName, corpusName).findAll {Map<String, Object> declaration ->
            Boolean.TRUE.equals(declaration.get('enabled'))
        } as List<Map<String, Object>>
        List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> declaration : declarations) {
            jobs.add(queueModelIndexJob(
                mauroModelId,
                String.valueOf(declaration.get('profileName')),
                String.valueOf(declaration.get('corpusName')),
                integerValue(requestMap, 'maxRows'),
                integerValue(requestMap, 'batchSize'),
                rebuildEmbeddings,
                [
                    accepted: true,
                    runWhenIndexingDisabled: runWhenIndexingDisabled,
                    source: 'manual-start'
                ] as Map<String, Object>
            ))
        }
        SemanticModelIndexOperationResponseDTO.fromMap([
            mauroModelId: mauroModelId?.toString(),
            profileName: profileName,
            corpusName: corpusName,
            status: jobs.isEmpty() ? 'skipped' : 'submitted',
            matched: declarations.size(),
            jobs: jobs,
            reason: jobs.isEmpty() ? 'no enabled semantic model index declarations matched this scope' : null
        ] as Map<String, Object>)
    }

    @Connectable
    @Override
    List<SemanticIndexJobDTO> jobs(boolean includeHistory) {
        List<Map<String, Object>> rows = includeHistory ?
            semanticRepository.jobs() :
            semanticRepository.jobs(['QUEUED', 'TO_RESTART', 'RUNNING'] as List<String>)
        SemanticIndexJobDTO.listFrom(rows)
    }

    @Connectable
    @Override
    SemanticIndexJobDTO jobStatus(UUID jobId) {
        SemanticIndexJobDTO.fromMap(semanticRepository.job(jobId))
    }

    @Connectable
    @Override
    SemanticIndexJobDTO cancelJob(UUID jobId) {
        SemanticIndexJobDTO.fromMap(semanticRepository.cancelJob(jobId, 'semantic indexing job was cancelled by API request'))
    }

    @Connectable
    @Override
    SemanticIndexJobDTO resumeJob(UUID jobId) {
        Map<String, Object> job = semanticRepository.job(jobId)
        String status = String.valueOf(job.get('status'))
        if (['SUCCEEDED', 'FAILED', 'CANCELLED', 'RUNNING'].contains(status)) {
            return SemanticIndexJobDTO.fromMap(job)
        }
        SemanticIndexJobDTO.fromMap(resumeRecoverableJob(job))
    }

    @Connectable
    @Override
    String jobEvents(UUID jobId) {
        semanticRepository.jobEvents(jobId)
    }

    @Override
    Publisher<String> followJobEvents(UUID jobId, Long afterSequence) {
        Flux.defer({
            long startAfter = Math.max(afterSequence ?: 0L, 0L)
            Flux.interval(Duration.ZERO, Duration.ofSeconds(1L))
                .scan([after: startAfter, done: false, lines: Collections.<String>emptyList()] as Map<String, Object>) {Map<String, Object> state, Long tick ->
                    if (Boolean.TRUE.equals(state.get('done'))) {
                        return state
                    }
                    long after = ((Number) state.get('after')).longValue()
                    List<String> lines = semanticRepository.jobEventLinesAfter(jobId, after)
                    long nextAfter = after
                    for (String line : lines) {
                        Long sequence = sequenceFromLine(line)
                        if (sequence != null) {
                            nextAfter = Math.max(nextAfter, sequence.longValue())
                        }
                    }
                    Map<String, Object> job = semanticRepository.job(jobId)
                    boolean terminal = terminalJobStatus(String.valueOf(job.get('status')))
                    if (lines.isEmpty() && !terminal) {
                        lines = [semanticRepository.jobSnapshotLine(jobId)] as List<String>
                    }
                    boolean done = terminal && lines.isEmpty()
                    [after: nextAfter, done: done, lines: lines] as Map<String, Object>
                }
                .takeUntil {Map<String, Object> state -> Boolean.TRUE.equals(state.get('done'))}
                .flatMapIterable {Map<String, Object> state ->
                    ((List<String>) state.get('lines')).collect {String line -> line + '\n'} as List<String>
                }
        })
    }

    @Connectable
    @Override
    List<SemanticIndexJobDTO> recoverInterruptedJobs() {
        List<Map<String, Object>> recoverableJobs = semanticRepository.recoverableJobs()
        recoverableJobs.sort {Map<String, Object> left, Map<String, Object> right ->
            String.valueOf(left.get('createdAt')) <=> String.valueOf(right.get('createdAt'))
        }
        List<Map<String, Object>> recovered = new ArrayList<Map<String, Object>>()
        Set<String> recoveredDeclarations = new LinkedHashSet<String>()
        for (Map<String, Object> job : recoverableJobs) {
            String declarationKey = "${job.get('corpusName')}:${job.get('mauroModelId')}:${job.get('profileName')}".toString()
            if (recoveredDeclarations.contains(declarationKey)) {
                UUID duplicateJobId = UUID.fromString(String.valueOf(job.get('jobId')))
                recovered.add(semanticRepository.cancelJob(
                    duplicateJobId,
                    'semantic indexing job was superseded by an equivalent recovered job'
                ))
                continue
            }
            UUID jobId = UUID.fromString(String.valueOf(job.get('jobId')))
            UUID mauroModelId = UUID.fromString(String.valueOf(job.get('mauroModelId')))
            String profileName = String.valueOf(job.get('profileName'))
            String corpusName = String.valueOf(job.get('corpusName') ?: 'catalogue-items')
            if (modelIndexReady(mauroModelId, profileName, corpusName)) {
                recovered.add(semanticRepository.cancelJob(
                    jobId,
                    'semantic indexing job recovery skipped because the model index declaration is already READY'
                ))
                continue
            }
            recoveredDeclarations.add(declarationKey)
            recovered.add(resumeRecoverableJob(job))
        }
        SemanticIndexJobDTO.listFrom(recovered)
    }

    private boolean modelIndexReady(UUID mauroModelId, String profileName, String corpusName) {
        Map<String, Object> declaration = semanticRepository.modelIndex(mauroModelId, profileName, corpusName)
        declaration != null &&
            Boolean.TRUE.equals(declaration.get('enabled')) &&
            String.valueOf(declaration.get('status')) == 'READY'
    }

    private List<Map<String, Object>> matchingModelIndexes(UUID mauroModelId, String profileName, String corpusName) {
        semanticRepository.modelIndexes().findAll {Map<String, Object> declaration ->
            declaration.get('mauroModelId') == mauroModelId?.toString() &&
                (profileName == null || declaration.get('profileName') == profileName) &&
                (corpusName == null || declaration.get('corpusName') == corpusName)
        } as List<Map<String, Object>>
    }

    private Map<String, Object> queueModelIndexJob(UUID mauroModelId,
                                                   String profileName,
                                                   String corpusName,
                                                   Integer maxRows,
                                                   Integer batchSize,
                                                   boolean rebuildEmbeddings,
                                                   Map<String, Object> metadata = null) {
        Map<String, Object> activeJob = semanticRepository.activeJobForModelIndex(mauroModelId, profileName, corpusName)
        if (activeJob != null) {
            String activeStatus = String.valueOf(activeJob.get('status'))
            if (activeStatus == 'RUNNING') {
                semanticRepository.markModelIndexStale(mauroModelId, profileName, corpusName, 'semantic model index changed while job was running')
                activeJob.put('accepted', true)
                activeJob.put('coalesced', true)
                activeJob.put('followUpRequired', true)
                activeJob.put('reason', 'equivalent semantic indexing job is already running; declaration marked stale for a follow-up pass')
                if (metadata != null) {
                    activeJob.putAll(metadata)
                }
                return activeJob
            }
            activeJob.put('accepted', true)
            activeJob.put('deduplicated', true)
            activeJob.put('reason', 'equivalent semantic indexing job is already queued')
            if (metadata != null) {
                activeJob.putAll(metadata)
            }
            return activeJob
        }

        UUID jobId = semanticRepository.createIndexJob(
            mauroModelId,
            profileName,
            corpusName,
            rebuildEmbeddings,
            maxRows,
            batchSize
        )
        submitIndexJob(
            jobId,
            mauroModelId,
            profileName,
            corpusName,
            maxRows,
            batchSize,
            rebuildEmbeddings,
            'QUEUED'
        )
        Map<String, Object> job = semanticRepository.job(jobId)
        if (metadata != null) {
            job.putAll(metadata)
        }
        job
    }

    private Map<String, Object> resumeRecoverableJob(Map<String, Object> job) {
        UUID jobId = UUID.fromString(String.valueOf(job.get('jobId')))
        UUID mauroModelId = UUID.fromString(String.valueOf(job.get('mauroModelId')))
        String profileName = String.valueOf(job.get('profileName'))
        String corpusName = String.valueOf(job.get('corpusName') ?: 'catalogue-items')
        String previousStatus = String.valueOf(job.get('status'))
        semanticRepository.markModelIndexStale(mauroModelId, profileName, corpusName, 'application restarted while semantic indexing was active')
        if (previousStatus == 'RUNNING') {
            semanticRepository.updateJobStatus(jobId, 'INTERRUPTED', [
                stage: 'interrupted',
                reason: 'application restarted while job was running',
                interruptedAt: new Date().toInstant().toString()
            ] as Map<String, Object>, null)
        }
        semanticRepository.updateJobStatus(jobId, 'TO_RESTART', [
            stage: 'to_restart',
            previousStatus: previousStatus,
            reason: previousStatus == 'QUEUED' ?
                'submitting queued semantic indexing job after application restart' :
                'application restarted while semantic indexing was active; rerunning declaration from current catalogue state',
            restartedAt: new Date().toInstant().toString()
        ] as Map<String, Object>, null)
        submitIndexJob(
            jobId,
            mauroModelId,
            profileName,
            corpusName,
            (Integer) job.get('maxRows'),
            (Integer) job.get('batchSize'),
            Boolean.TRUE.equals(job.get('rebuildEmbeddings')),
            'TO_RESTART'
        )
        semanticRepository.job(jobId)
    }

    private void submitIndexJob(UUID jobId,
                                UUID mauroModelId,
                                String profileName,
                                String corpusName,
                                Integer maxRows,
                                Integer batchSize,
                                boolean rebuildEmbeddings,
                                String submitStatus) {
        semanticRepository.updateJobStatus(jobId, submitStatus, [
            stage: 'submitted_to_executor',
            reason: 'semantic indexing job submitted to executor',
            submittedAt: new Date().toInstant().toString()
        ] as Map<String, Object>, null)
        log.info('Semantic model index job {} submitted to executor for model {} profile {} status={}', jobId, mauroModelId, profileName, submitStatus)
        executorService.submit({
            log.info('Semantic model index job {} executor task entered for model {} profile {}', jobId, mauroModelId, profileName)
            try {
                semanticRepository.updateJobStatus(jobId, submitStatus, [
                    stage: 'executor_task_entered',
                    reason: 'semantic indexing executor task entered',
                    enteredAt: new Date().toInstant().toString()
                ] as Map<String, Object>, null)
                runModelIndexJob(
                    jobId,
                    mauroModelId,
                    profileName,
                    corpusName,
                    maxRows,
                    batchSize,
                    rebuildEmbeddings
                )
            } catch (Throwable t) {
                String message = t.message ?: t.class.name
                semanticRepository.updateJobStatus(jobId, 'FAILED', [
                    stage: 'executor_task_failed',
                    reason: message
                ] as Map<String, Object>, message)
                log.error('Semantic model index job {} executor task failed before job runner handled it', jobId, t)
            }
        } as Runnable)
    }

    private Map<String, Object> runModelIndexJob(UUID jobId,
                                                 UUID mauroModelId,
                                                 String profileName,
                                                 String corpusName,
                                                 Integer maxRows,
                                                 Integer batchSize,
                                                 boolean force) {
        try {
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'running',
                reason: 'semantic indexing worker started',
                startedAt: new Date().toInstant().toString()
            ] as Map<String, Object>)
            log.info('Semantic model index job {} worker started for model {} profile {}', jobId, mauroModelId, profileName)
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'updating_model_index_status',
                reason: 'marking model index declaration as indexing',
                modelId: mauroModelId.toString(),
                profileName: profileName
            ] as Map<String, Object>)
            semanticRepository.updateModelIndexStatus(mauroModelId, profileName, corpusName, 'INDEXING')
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'model_index_status_updated',
                reason: 'model index declaration marked as indexing',
                modelId: mauroModelId.toString(),
                profileName: profileName
            ] as Map<String, Object>)
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'looking_up_embedding_profile',
                reason: 'resolving embedding profile for indexing',
                profileName: profileName
            ] as Map<String, Object>)
            EmbeddingProfile profile = semanticRepository.findProfileByName(profileName)
            if (profile == null) {
                throw new IllegalArgumentException("No enabled semantic embedding profile named ${profileName}")
            }
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'embedding_profile_resolved',
                reason: 'embedding profile resolved',
                profileName: profile.name,
                provider: profile.provider,
                embeddingModel: profile.embeddingModel,
                dimension: profile.dimension
            ] as Map<String, Object>)
            log.info('Semantic model index job {} resolved embedding profile {} provider={} model={}', jobId, profile.name, profile.provider, profile.embeddingModel)
            semanticRepository.updateJobStatus(jobId, 'RUNNING', [
                stage: 'starting_index_rebuild',
                reason: 'entering semantic index rebuild',
                modelId: mauroModelId.toString(),
                profileName: profile.name,
                corpusName: corpusName ?: 'catalogue-items',
                rebuildEmbeddings: force
            ] as Map<String, Object>)
            Map<String, Object> result = rebuildCatalogueIndexWithProfiles(
                [profile] as List<EmbeddingProfile>,
                "model-${mauroModelId}-${profileName}".toString(),
                corpusName ?: 'catalogue-items',
                Collections.<String>emptyList(),
                mauroModelId,
                maxRows,
                batchSize,
                force,
                { -> semanticRepository.jobCancelled(jobId) } as Closure<Boolean>,
                {Map<String, Object> progress -> semanticRepository.updateJobStatus(jobId, 'RUNNING', progress)} as Consumer<Map<String, Object>>
            )
            boolean changedDuringRun = semanticRepository.modelIndexChangedDuringRun(mauroModelId, profileName, corpusName)
            boolean stillNeedsRefresh = changedDuringRun
            result.put('changedDuringRun', changedDuringRun)
            result.put('stillNeedsRefresh', stillNeedsRefresh)
            if (stillNeedsRefresh) {
                semanticRepository.markModelIndexStale(mauroModelId, profileName, corpusName, 'semantic model index changed during indexing; queuing follow-up pass')
                result.put('followUpQueued', true)
            } else {
                semanticRepository.updateModelIndexStatus(mauroModelId, profileName, corpusName, 'READY')
            }
            semanticRepository.updateJobStatus(jobId, 'SUCCEEDED', result)
            if (stillNeedsRefresh) {
                queueModelIndexJob(
                    mauroModelId,
                    profileName,
                    corpusName ?: 'catalogue-items',
                    maxRows,
                    batchSize,
                    false,
                    [
                        accepted: true,
                        source: 'follow-up',
                        reason: 'semantic model index changed during previous indexing pass'
                    ] as Map<String, Object>
                )
            }
            semanticRepository.job(jobId)
        } catch (CancellationException e) {
            String message = e.message ?: 'semantic indexing job was cancelled'
            semanticRepository.updateModelIndexStatus(mauroModelId, profileName, corpusName, 'STALE')
            semanticRepository.updateJobStatus(jobId, 'CANCELLED', [
                stage: 'cancelled',
                reason: message
            ] as Map<String, Object>, null)
            semanticRepository.job(jobId)
        } catch (Throwable t) {
            String message = t.message ?: t.class.name
            semanticRepository.updateModelIndexStatus(mauroModelId, profileName, corpusName, 'FAILED', message)
            semanticRepository.updateJobStatus(jobId, 'FAILED', null, message)
            log.error('Semantic model index job {} failed for model {} profile {}', jobId, mauroModelId, profileName, t)
            semanticRepository.job(jobId)
        }
    }

    private static String stringValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        value == null ? null : String.valueOf(value)
    }

    private static String requiredString(Map<String, Object> request, String key) {
        String value = stringValue(request, key)
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field ${key}")
        }
        value.trim()
    }

    private static UUID uuidValue(Map<String, Object> request, String key) {
        String value = stringValue(request, key)
        value == null || value.trim().isEmpty() ? null : UUID.fromString(value)
    }

    private static Integer integerValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null
        }
        value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(String.valueOf(value))
    }

    private static boolean booleanValue(Map<String, Object> request, String key, boolean fallback) {
        Object value = request == null ? null : request.get(key)
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }

    private static boolean terminalJobStatus(String status) {
        ['SUCCEEDED', 'FAILED', 'CANCELLED'].contains(status)
    }

    private static Long sequenceFromLine(String line) {
        if (line == null) {
            return null
        }
        java.util.regex.Matcher matcher = line =~ /"sequence":(\d+)/
        matcher.find() ? Long.valueOf(matcher.group(1)) : null
    }
}
