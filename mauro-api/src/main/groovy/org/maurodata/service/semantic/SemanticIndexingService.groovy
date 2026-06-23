package org.maurodata.service.semantic

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

    SemanticIndexingService(SemanticRepository semanticRepository,
                            EmbeddingProviderRegistry embeddingProviderRegistry,
                            @Value('${chat.semantic.embeddings.batch-size:512}') Integer embeddingBatchSize,
                            @Value('${chat.semantic.embeddings.defer-vector-index-threshold:10000}') Integer deferVectorIndexThreshold) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
        this.embeddingBatchSize = Math.max(embeddingBatchSize ?: 32, 1)
        this.deferVectorIndexThreshold = Math.max(deferVectorIndexThreshold ?: 0, 0)
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

        semanticRepository.updateIndexStatus(indexName, 'INDEXING')
        log.info(
            'Semantic index rebuild starting indexName={} corpusName={} profiles={} domainTypes={} mauroModelId={} maxRows={} batchSize={} force={}',
            indexName,
            corpusName,
            profiles.collect {EmbeddingProfile profile -> profile.name},
            domainTypes ?: [],
            mauroModelId,
            maxRows,
            effectiveBatchSize,
            Boolean.valueOf(force)
        )

        int chunkCount = semanticRepository.countCatalogueCandidateChunks(corpusName, domainTypes, mauroModelId, maxRows)
        log.info('Semantic index rebuild selected {} candidate chunks for indexName={}', Integer.valueOf(chunkCount), indexName)
        int changedChunkCount = semanticRepository.reconcileCatalogueChunks(corpusName, domainTypes, mauroModelId, maxRows)
        int embeddingCount = 0
        int skippedEmbeddingCount = 0
        Map<String, Integer> profileEmbeddingCounts = new LinkedHashMap<String, Integer>()
        Map<String, Integer> profileSkippedCounts = new LinkedHashMap<String, Integer>()
        log.info('Semantic index rebuild reconciled chunk records for indexName={} candidateChunks={} changedChunkRows={}', indexName, Integer.valueOf(chunkCount), Integer.valueOf(changedChunkCount))

        try {
            for (EmbeddingProfile profile : profiles) {
                EmbeddingProvider provider = embeddingProviderRegistry.providerFor(profile)
                int staleEmbeddings = semanticRepository.deleteStaleEmbeddings(profile)
                int chunksToEmbedCount = semanticRepository.countChunksNeedingEmbedding(profile, corpusName, domainTypes, mauroModelId, maxRows, force)
                int skippedForProfile = chunkCount - chunksToEmbedCount
                skippedEmbeddingCount += skippedForProfile
                profileSkippedCounts.put(profile.name, skippedForProfile)
                profileEmbeddingCounts.put(profile.name, 0)
                log.info(
                    'Semantic index profile {} has {} chunks needing embeddings, {} unchanged chunks skipped, {} stale embeddings removed',
                    profile.name,
                    Integer.valueOf(chunksToEmbedCount),
                    Integer.valueOf(skippedForProfile),
                    Integer.valueOf(staleEmbeddings)
                )

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
                    existingEmbeddingCount == 0L
                if (deferVectorIndex) {
                    log.info(
                        'Semantic index profile {} deferring HNSW index maintenance for initial load of {} embeddings (threshold={})',
                        profile.name,
                        Integer.valueOf(chunksToEmbedCount),
                        Integer.valueOf(deferVectorIndexThreshold)
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
                UUID lastForceChunkId = null
                try {
                    for (;;) {
                        List<SemanticChunk> batch = semanticRepository.nextChunksNeedingEmbedding(
                            profile,
                            corpusName,
                            domainTypes,
                            mauroModelId,
                            maxRows,
                            force,
                            effectiveBatchSize,
                            lastForceChunkId
                        )
                        if (batch.isEmpty()) {
                            break
                        }
                        if (force) {
                            lastForceChunkId = batch.get(batch.size() - 1).id
                        }
                        batchNumber++
                        List<String> texts = batch.collect {SemanticChunk chunk -> chunk.sourceText} as List<String>
                        List<float[]> embeddings = provider.embed(profile, texts)
                        if (embeddings.size() != batch.size()) {
                            throw new IllegalStateException("Embedding provider ${profile.provider}/${profile.embeddingModel} returned ${embeddings.size()} embeddings for ${batch.size()} chunks")
                        }
                        semanticRepository.upsertEmbeddings(batch, profile, embeddings)
                        embeddingCount += batch.size()
                        embeddedForProfile += batch.size()
                        profileEmbeddingCounts.put(profile.name, embeddedForProfile)
                        if (batchNumber == 1 || batchNumber % 25 == 0 || batchNumber == totalBatches) {
                            log.info(
                                'Semantic index profile {} progress: embedded {}/{} chunks (batch {}/{})',
                                profile.name,
                                Integer.valueOf(embeddedForProfile),
                                Integer.valueOf(chunksToEmbedCount),
                                Integer.valueOf(batchNumber),
                                Integer.valueOf(totalBatches)
                            )
                        }
                    }
                } finally {
                    if (deferVectorIndex) {
                        log.info('Semantic index profile {} rebuilding HNSW index after embedding load', profile.name)
                        semanticRepository.createVectorIndex(profile)
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
            profileEmbeddings: profileEmbeddingCounts,
            profileSkippedEmbeddings: profileSkippedCounts,
            batchSize: effectiveBatchSize,
            force: force,
            elapsedMs: System.currentTimeMillis() - start
        ] as Map<String, Object>
        log.info('Semantic index rebuild completed: {}', result)
        result
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
