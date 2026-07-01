package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton

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

    SemanticIndexingService(SemanticRepository semanticRepository,
                            EmbeddingProviderRegistry embeddingProviderRegistry,
                            @Value('${chat.semantic.embeddings.batch-size:512}') Integer embeddingBatchSize,
                            @Value('${chat.semantic.embeddings.defer-vector-index-threshold:10000}') Integer deferVectorIndexThreshold,
                            @Value('${chat.semantic.embeddings.defer-vector-index-with-existing-embeddings:false}') Boolean deferVectorIndexWithExistingEmbeddings,
                            @Value('${chat.semantic.embeddings.rebuild-vector-index-after-partial-load:false}') Boolean rebuildVectorIndexAfterPartialLoad,
                            @Value('${chat.semantic.embeddings.reuse-duplicate-content-before-embedding:false}') Boolean reuseDuplicateContentBeforeEmbedding,
                            @Value('${chat.semantic.embeddings.adaptive-min-batch-size:128}') Integer minimumAdaptiveBatchSize,
                            @Value('${chat.semantic.embeddings.progress-log-interval-seconds:30}') Integer progressLogIntervalSeconds) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
        this.embeddingBatchSize = Math.max(embeddingBatchSize ?: 32, 1)
        this.deferVectorIndexThreshold = Math.max(deferVectorIndexThreshold ?: 0, 0)
        this.deferVectorIndexWithExistingEmbeddings = deferVectorIndexWithExistingEmbeddings == true
        this.rebuildVectorIndexAfterPartialLoad = rebuildVectorIndexAfterPartialLoad == true
        this.reuseDuplicateContentBeforeEmbedding = reuseDuplicateContentBeforeEmbedding == true
        this.minimumAdaptiveBatchSize = Math.max(minimumAdaptiveBatchSize ?: 128, 1)
        this.progressLogIntervalMillis = Math.max(progressLogIntervalSeconds ?: 30, 1) * 1000L
    }

    @Connectable
    @Override
    Map<String, Object> rebuildCatalogueIndex(String indexName = 'catalogue-items-default',
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

        boolean contextRefreshed = semanticRepository.refreshAdministeredItemContextIfExists()
        semanticRepository.updateIndexStatus(indexName, 'INDEXING')
        log.info(
            'Semantic index rebuild starting indexName={} corpusName={} profiles={} domainTypes={} mauroModelId={} maxRows={} batchSize={} force={} administeredItemContextRefreshed={}',
            indexName,
            corpusName,
            profiles.collect {EmbeddingProfile profile -> profile.name},
            domainTypes ?: [],
            mauroModelId,
            maxRows,
            effectiveBatchSize,
            Boolean.valueOf(force),
            Boolean.valueOf(contextRefreshed)
        )

        int chunkCount = semanticRepository.countCatalogueCandidateChunks(corpusName, domainTypes, mauroModelId, maxRows)
        log.info('Semantic index rebuild selected {} candidate chunks for indexName={}', Integer.valueOf(chunkCount), indexName)
        int changedChunkCount = semanticRepository.reconcileCatalogueChunks(corpusName, domainTypes, mauroModelId, maxRows)
        int syncedEmbeddingGroups = semanticRepository.syncEmbeddingChunkGroups(corpusName, domainTypes, mauroModelId, maxRows)
        int embeddingCount = 0
        int skippedEmbeddingCount = 0
        int reusedEmbeddingCount = 0
        Map<String, Integer> profileEmbeddingCounts = new LinkedHashMap<String, Integer>()
        Map<String, Integer> profileSkippedCounts = new LinkedHashMap<String, Integer>()
        Map<String, Integer> profileReusedCounts = new LinkedHashMap<String, Integer>()
        log.info('Semantic index rebuild reconciled chunk records for indexName={} candidateChunks={} changedChunkRows={} syncedEmbeddingChunkGroups={}', indexName, Integer.valueOf(chunkCount), Integer.valueOf(changedChunkCount), Integer.valueOf(syncedEmbeddingGroups))

        try {
            for (EmbeddingProfile profile : profiles) {
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
            int splitSize = Math.max(batch.size().intdiv(2), minimumAdaptiveBatchSize)
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
    List<Map<String, Object>> reconcileDeclaredIndexes() {
        List<Map<String, Object>> indexes = semanticRepository.indexes()
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> index : indexes) {
            String indexName = String.valueOf(index.get('name'))
            List<?> enabledProfiles = index.get('enabledProfiles') instanceof List ? (List<?>) index.get('enabledProfiles') : []
            if (enabledProfiles.isEmpty()) {
                log.info('Skipping semantic index {} because it has no enabled linked profiles', indexName)
                continue
            }
            results.add(rebuildCatalogueIndex(
                indexName,
                String.valueOf(index.get('corpusName') ?: 'catalogue-items'),
                Collections.<String>emptyList(),
                null,
                null,
                null,
                false
            ))
        }
        results
    }

    @Connectable
    @Override
    boolean hasEmbeddings(String indexName = 'catalogue-items-default') {
        semanticRepository.hasEmbeddings(indexName)
    }
}
