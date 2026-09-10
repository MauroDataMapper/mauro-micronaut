package org.maurodata.plugin.chat.semantic

import org.maurodata.service.semantic.EmbeddingProfile
import spock.lang.Specification

class SetSemanticIndexingServiceSpec extends Specification {
    void 'disabled declarations do not maintain indexes but explicit derived requests still run'() {
        given:
        def profile = new EmbeddingProfile(id: UUID.randomUUID(), name: 'test', provider: 'local', embeddingModel: 'fixture', dimension: 3, distanceMetric: 'cosine')
        UUID owner = UUID.randomUUID()
        Map queued = [embedding_profile_id: profile.id, profile_name: profile.name, corpus_name: 'catalogue-items',
                      mauro_model_id: owner, explicit_request: false]
        List<UUID> rebuilt = []
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override boolean indexingEnabled() { true }
            @Override boolean autoReconcileEnabled() { true }
            @Override List<Map<String, Object>> modelIndexes() {
                [[enabled: false, profileName: 'test', mauroModelId: owner, corpusName: 'catalogue-items']]
            }
            @Override EmbeddingProfile findProfileByName(String name) { profile }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override void recoverInterrupted() { }
            @Override List<Map<String, Object>> queued() { [queued] }
        }
        def service = new SetSemanticIndexingService(repository, indexes, 8, 0.05D, 0.2D, false) {
            @Override Map<String, Object> rebuildSetIndexes(EmbeddingProfile p, String corpus, UUID modelId) {
                rebuilt.add(modelId)
                [status: 'READY']
            }
        }
        when:
        service.reconcileSetIndexes()
        then:
        rebuilt.empty
        when:
        queued.explicit_request = true
        service.reconcileSetIndexes()
        then:
        rebuilt == [owner]
    }

    void 'each poll processes a bounded snapshot and leaves remaining requests for the next poll'() {
        given:
        def profile = new EmbeddingProfile(id: UUID.randomUUID(), name: 'test', distanceMetric: 'cosine')
        List<UUID> owners = (1..3).collect { UUID.randomUUID() }
        List<Map<String, Object>> queue = owners.collect { UUID owner ->
            [embedding_profile_id: profile.id, profile_name: profile.name, corpus_name: 'catalogue-items',
             mauro_model_id: owner, explicit_request: true]
        }
        List<UUID> rebuilt = []
        int profileReads = 0
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override boolean indexingEnabled() { true }
            @Override List<Map<String, Object>> modelIndexes() { [] }
            @Override EmbeddingProfile findProfileByName(String name) { profileReads++; profile }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override void recoverInterrupted() { }
            @Override List<Map<String, Object>> queued() { new ArrayList<>(queue) }
        }
        def service = new SetSemanticIndexingService(repository, indexes, 8, 0.05D, 0.2D, false) {
            @Override Map<String, Object> rebuildSetIndexes(EmbeddingProfile p, String corpus, UUID modelId) {
                rebuilt.add(modelId)
                queue.removeAll { it.mauro_model_id == modelId }
                [status: 'READY']
            }
        }
        service.jobsPerPoll = 2
        when:
        service.reconcileSetIndexes()
        then:
        profileReads == 1
        rebuilt == owners.take(2)
        queue*.mauro_model_id == owners.drop(2)
        when:
        service.reconcileSetIndexes()
        then:
        profileReads == 2
        rebuilt == owners
        queue.empty
    }

    void 'freshness reads revision and configuration state without loading member vectors'() {
        given:
        def profile = new EmbeddingProfile(id: UUID.randomUUID(), name: 'test', provider: 'local', embeddingModel: 'fixture', dimension: 3, distanceMetric: 'cosine')
        Map state = [stale: false]
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override List<SetSemanticMemberEmbedding> setSemanticMemberEmbeddings(EmbeddingProfile p, String corpus, UUID owner) {
                throw new AssertionError('Freshness must not load or calculate embeddings')
            }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override Map<String, Object> state(EmbeddingProfile p, String corpus, UUID owner) { state }
        }
        def service = new SetSemanticIndexingService(repository, indexes, 8, 0.05D, 0.2D, false)
        state.configuration_fingerprint = service.configurationFingerprint(profile)
        expect:
        !service.needsRefresh(profile, 'catalogue-items', UUID.randomUUID())
        when:
        state.stale = true
        then:
        service.needsRefresh(profile, 'catalogue-items', UUID.randomUUID())
        when:
        state.stale = false
        state.configuration_fingerprint = 'previous algorithm'
        then:
        service.needsRefresh(profile, 'catalogue-items', UUID.randomUUID())
    }
}
