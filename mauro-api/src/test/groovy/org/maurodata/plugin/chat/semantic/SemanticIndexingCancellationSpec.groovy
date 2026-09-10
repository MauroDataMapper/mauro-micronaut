package org.maurodata.plugin.chat.semantic

import org.maurodata.service.semantic.EmbeddingProfile
import spock.lang.Specification

import java.util.concurrent.CancellationException
import java.util.function.Consumer

class SemanticIndexingCancellationSpec extends Specification {
    void 'cancellation during chunk reconciliation stops before subsequent database and embedding work'() {
        given:
        boolean cancelled = false
        List<String> work = []
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override boolean refreshAdministeredItemContextIfExists() { true }
            @Override void updateIndexStatus(String name, String status) { }
            @Override int countCatalogueCandidateChunks(String corpus, List<String> types, UUID owner, Integer limit) { 1 }
            @Override Map<String, Integer> reconcileCatalogueChunksDetailed(String corpus, List<String> types, UUID owner, Integer limit) {
                work.add('reconcile')
                cancelled = true
                [upsertedChunks: 0, deletedChunks: 0, changedChunks: 0]
            }
            @Override int syncEmbeddingChunkGroups(String corpus, List<String> types, UUID owner, Integer limit) {
                work.add('sync')
                0
            }
            @Override List<Map<String, Object>> cancelActiveJobs(String reason) { [] }
        }
        def service = new SemanticIndexingService(repository, new EmbeddingProviderRegistry([]), 512, 10000,
            false, false, false, 128, 30, 'test', 1)
        when:
        service.rebuildCatalogueIndexWithProfiles([new EmbeddingProfile(name: 'test')], 'test', 'catalogue-items', [],
            UUID.randomUUID(), null, null, false, { -> cancelled }, null)
        then:
        thrown(CancellationException)
        work == ['reconcile']
        cleanup:
        service.shutdownExecutor()
    }

    void 'failed freshness checks do not mark READY declarations stale or queue rebuilds (#sqlState)'() {
        given:
        UUID owner = UUID.randomUUID()
        def failure = new java.sql.SQLException('freshness check failed', sqlState)
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override boolean indexingEnabled() { true }
            @Override List<Map<String, Object>> modelIndexes() {
                [[enabled: true, mauroModelId: owner, profileName: 'test', corpusName: 'catalogue-items', status: 'READY']]
            }
            @Override boolean modelIndexNeedsRefresh(String corpus, UUID modelId, String profileName) { throw failure }
            @Override void markModelIndexStale(UUID modelId, String profileName, String corpus, String reason) {
                throw new AssertionError('Failed checks must not invalidate the declaration')
            }
            @Override List<Map<String, Object>> cancelActiveJobs(String reason) { [] }
        }
        def service = new SemanticIndexingService(repository, new EmbeddingProviderRegistry([]), 512, 10000,
            false, false, false, 128, 30, 'test', 1)
        when:
        service.reconcileDeclaredIndexes()
        then:
        def actual = thrown(java.sql.SQLException)
        actual.is(failure)
        cleanup:
        service.shutdownExecutor()
        where:
        sqlState << ['57014', '08006']
    }

    void 'thread interruption stops preparation without consuming the interrupt flag'() {
        given:
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override List<Map<String, Object>> cancelActiveJobs(String reason) { [] }
        }
        def service = new SemanticIndexingService(repository, new EmbeddingProviderRegistry([]), 512, 10000,
            false, false, false, 128, 30, 'test', 1)
        when:
        Thread.currentThread().interrupt()
        service.rebuildCatalogueIndexWithProfiles([new EmbeddingProfile(name: 'test')], 'test')
        then:
        thrown(CancellationException)
        Thread.currentThread().isInterrupted()
        cleanup:
        Thread.interrupted()
        service.shutdownExecutor()
    }
}
