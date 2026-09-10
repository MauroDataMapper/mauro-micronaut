package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import org.maurodata.service.semantic.EmbeddingProfile

import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
@CompileStatic
@Singleton
class SetSemanticIndexingService implements ApplicationEventListener<StartupEvent> {
    private final SemanticRepository semanticRepository
    private final SetSemanticIndexRepository indexRepository
    private final SetSemanticCentroidCalculator calculator = new SetSemanticCentroidCalculator()
    private final int maxLocalRegionCount
    private final double coverageRadius
    private final double neighbourhoodRadius
    private final boolean includeIdentifiers
    @Value('${chat.semantic.set-index.jobs-per-poll:100}')
    int jobsPerPoll = 100
    private final AtomicBoolean working = new AtomicBoolean(false)

    SetSemanticIndexingService(SemanticRepository semanticRepository, SetSemanticIndexRepository indexRepository,
                               @Value('${chat.semantic.set-index.max-local-region-count:16}') int maxLocalRegionCount,
                               @Value('${chat.semantic.set-index.coverage-radius:0.15}') double coverageRadius,
                               @Value('${chat.semantic.set-index.neighbourhood-radius:0.20}') double neighbourhoodRadius,
                               @Value('${chat.semantic.set-index.include-identifiers:false}') boolean includeIdentifiers) {
        this.semanticRepository = semanticRepository
        this.indexRepository = indexRepository
        this.maxLocalRegionCount = maxLocalRegionCount
        this.coverageRadius = coverageRadius
        this.neighbourhoodRadius = neighbourhoodRadius
        this.includeIdentifiers = includeIdentifiers
        calculator.calculate([], maxLocalRegionCount, coverageRadius, neighbourhoodRadius) // Validate configuration on startup.
    }

    @Override
    void onApplicationEvent(StartupEvent event) {
        try { indexRepository.recoverInterrupted() }
        catch (Exception failure) { log.warn('Could not recover derived set indexes during startup', failure) }
    }

    String configurationFingerprint(EmbeddingProfile profile) {
        String input = [SetSemanticCentroidCalculator.ALGORITHM_VERSION, maxLocalRegionCount, coverageRadius,
            neighbourhoodRadius, includeIdentifiers, profile.id, profile.provider, profile.embeddingModel,
            profile.dimension, profile.distanceMetric].join('|')
        MessageDigest.getInstance('SHA-256').digest(input.getBytes('UTF-8')).encodeHex().toString()
    }

    boolean needsRefresh(EmbeddingProfile profile, String corpusName, UUID modelId) {
        Map<String, Object> state = indexRepository.state(profile, corpusName, modelId)
        state.stale == true || state.configuration_fingerprint != configurationFingerprint(profile)
    }

    Map<String, Object> requestRebuild(EmbeddingProfile profile, String corpusName, UUID scopeId) {
        List<UUID> scopes = semanticRepository.setIndexModelScopes(scopeId)
        if (!scopes) throw new IllegalArgumentException('No existing model scope found')
        for (UUID owner : scopes) {
            indexRepository.declare(profile, corpusName, owner, configurationFingerprint(profile))
            indexRepository.request(profile, corpusName, owner)
        }
        [status: 'QUEUED', modelScopes: scopes] as Map<String, Object>
    }

    /** Process a bounded queue snapshot; no calls to an embedding provider or catalogue rebuild. */
    @Scheduled(fixedDelay = '${chat.semantic.set-index.poll-interval:10s}', initialDelay = '10s')
    void reconcileSetIndexes() {
        if (!working.compareAndSet(false, true)) return
        try {
            if (!semanticRepository.indexingEnabled()) return
            indexRepository.recoverInterrupted()
            Map<String, EmbeddingProfile> profiles = [:]
            Set<String> declaredScopes = new LinkedHashSet<String>()
            List<Map<String, Object>> declarations = semanticRepository.modelIndexes().findAll { it.enabled == true }
            for (Map<String, Object> declaration : declarations) {
                String profileName = declaration.profileName.toString()
                if (!profiles.containsKey(profileName)) profiles.put(profileName, semanticRepository.findProfileByName(profileName))
                EmbeddingProfile profile = profiles.get(profileName)
                if (profile == null || profile.distanceMetric != 'cosine') continue
                String corpus = declaration.corpusName?.toString() ?: 'catalogue-items'
                for (UUID owner : semanticRepository.setIndexModelScopes(UUID.fromString(declaration.mauroModelId.toString()))) {
                    declaredScopes.add([profile.id, corpus, owner].join('|'))
                    if (semanticRepository.autoReconcileEnabled()) {
                        indexRepository.declare(profile, corpus, owner, configurationFingerprint(profile))
                    }
                }
            }
            List<Map<String, Object>> queue = indexRepository.queued()
            int attempted = 0
            int eligible = 0
            long started = System.currentTimeMillis()
            for (Map<String, Object> queued : queue) {
                String profileName = queued.profile_name.toString()
                if (!profiles.containsKey(profileName)) profiles.put(profileName, semanticRepository.findProfileByName(profileName))
                EmbeddingProfile profile = profiles.get(profileName)
                String key = [queued.embedding_profile_id, queued.corpus_name, queued.mauro_model_id].join('|')
                if (profile != null && (queued.explicit_request == true || declaredScopes.contains(key))) {
                    eligible++
                    if (attempted < Math.max(jobsPerPoll, 1) && !Thread.currentThread().isInterrupted()) {
                        rebuildSetIndexes(profile, queued.corpus_name.toString(), (UUID) queued.mauro_model_id)
                        attempted++
                    }
                }
            }
            if (queue) log.info('Derived set index poll finished queuedAtStart={} eligibleAtStart={} attempted={} deferredFromSnapshot={} elapsedMs={}',
                queue.size(), eligible, attempted, eligible - attempted, System.currentTimeMillis() - started)
        } catch (Exception failure) {
            log.error('Derived set index reconciliation failed', failure)
        } finally { working.set(false) }
    }

    Map<String, Object> rebuildSetIndexes(EmbeddingProfile profile, String corpusName, UUID modelId) {
        indexRepository.withScopeLock(profile, corpusName, modelId, { rebuildClaimed(profile, corpusName, modelId) })
    }

    private Map<String, Object> rebuildClaimed(EmbeddingProfile profile, String corpusName, UUID modelId) {
        Map<String, Object> job = indexRepository.claim(profile, corpusName, modelId)
        if (job == null) return indexRepository.state(profile, corpusName, modelId)
        try {
            if (profile.distanceMetric != 'cosine') throw new IllegalArgumentException('Set indexes require a cosine embedding profile')
            List<SetSemanticMemberEmbedding> members = semanticRepository.setSemanticMemberEmbeddings(profile, corpusName, modelId)
            Map<String, List<SetSemanticMemberEmbedding>> groups = members.findAll {
                includeIdentifiers || it.vectorFamily == 'meaning'
            }.groupBy { [it.setDomainType, it.setId, it.vectorFamily].join('|') } as Map<String, List<SetSemanticMemberEmbedding>>
            List<SetSemanticCentroid> centroids = []
            Map<String, Object> sets = [:]
            boolean incomplete = false
            String generatedAt = Instant.now().toString()
            groups.each { String key, List<SetSemanticMemberEmbedding> group ->
                List<SetSemanticMemberEmbedding> actualMembers = group.findAll { it.memberId != null }
                List<SetSemanticMemberEmbedding> eligible = actualMembers.findAll { it.memberText?.trim() }
                List<SetSemanticMemberEmbedding> usable = eligible.findAll { SetSemanticCentroidCalculator.isUsable(it.embedding) }
                List<SetSemanticCentroid> calculated = calculator.calculate(usable, maxLocalRegionCount, coverageRadius, neighbourhoodRadius)
                Map<String, Object> quality = [totalMemberCount: actualMembers.size(), eligibleMemberCount: eligible.size(),
                    currentEmbeddingMemberCount: usable.size(), representedMemberCount: usable.size(),
                    missingEmbeddingMemberCount: eligible.size() - usable.size(), incomplete: usable.size() < eligible.size(),
                    generatedAt: generatedAt, sourceRevision: job.revision, embeddingProfile: profile.name,
                    embeddingModel: profile.embeddingModel, configurationFingerprint: configurationFingerprint(profile)] as Map<String, Object>
                if (calculated) quality.putAll(calculated.first().metadata)
                quality.remove('seedMemberId') // Seed identity belongs to each vector, not the set generation.
                quality.put('invalidEmbeddingCount', eligible.count { it.embedding?.length > 0 && !SetSemanticCentroidCalculator.isUsable(it.embedding) })
                incomplete |= usable.size() < eligible.size()
                calculated.each { SetSemanticCentroid centroid ->
                    centroid.metadata.putAll(quality)
                    centroid.sourceFingerprint = configurationFingerprint(profile) + ':' + job.revision.toString()
                }
                sets.put(key, quality)
                centroids.addAll(calculated)
            }
            Map<String, Object> result = [setVectors: centroids.size(), sets: sets, generatedAt: generatedAt,
                incomplete: incomplete, algorithmVersion: SetSemanticCentroidCalculator.ALGORITHM_VERSION] as Map<String, Object>
            semanticRepository.createSetVectorIndex(profile)
            indexRepository.publish(job, centroids, result, incomplete)
            Map<String, Object> published = indexRepository.state(profile, corpusName, modelId)
            log.info('Derived set index attempt finished model={} profile={} status={} calculatedVectors={} incomplete={} stale={} sourceRevision={} currentRevision={}',
                modelId, profile.name, published.status, result.setVectors, incomplete, published.stale, job.revision, published.revision)
            published
        } catch (Throwable failure) {
            indexRepository.failed(job, failure)
            log.error('Derived set index failed model={} profile={}', modelId, profile.name, failure)
            indexRepository.state(profile, corpusName, modelId)
        }
    }
}
