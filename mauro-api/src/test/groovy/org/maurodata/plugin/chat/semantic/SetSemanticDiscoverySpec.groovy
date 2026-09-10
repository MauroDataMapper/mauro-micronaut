package org.maurodata.plugin.chat.semantic

import org.maurodata.service.semantic.EmbeddingProfile
import spock.lang.Specification
import java.util.function.BiPredicate

class SetSemanticDiscoverySpec extends Specification {
    void 'omitted dimensions merge independent rankings and keep paging independent of page size'() {
        given:
        UUID source = UUID.randomUUID()
        UUID first = UUID.randomUUID()
        UUID second = UUID.randomUUID()
        List<Integer> limits = []
        List<String> calls = []
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override EmbeddingProfile findProfileByName(String name) { new EmbeddingProfile(name: name, distanceMetric: 'cosine') }
            @Override List<Map<String, Object>> corpora(boolean internal) { [[name: 'a', enabled: true], [name: 'b', enabled: true]] }
            @Override List<SetSemanticCentroid> setSemanticCentroids(EmbeddingProfile p, String corpus, UUID id, String type) {
                calls.add(p.name + ':' + corpus)
                [new SetSemanticCentroid(setId: source, setDomainType: 'Terminology', vectorFamily: 'meaning', centroidKind: 'region', regionOrdinal: 1, mauroModelId: source)]
            }
            @Override List<SetSemanticCandidate> searchSetSemanticCandidates(EmbeddingProfile p, String corpus, List<SetSemanticCentroid> vectors, int limit) {
                limits.add(limit)
                [candidate(first, p.name == 'one' ? 0.99D : 0.60D), candidate(second, p.name == 'one' ? 0.90D : 0.70D)]
            }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override List<Map<String, Object>> discoveryDimensions(String profile, String corpus) {
                [[profile: 'one', corpus: 'a'], [profile: 'two', corpus: 'b']].findAll {
                    (profile == null || it.profile == profile) && (corpus == null || it.corpus == corpus)
                }
            }
            @Override Map<String, Object> state(EmbeddingProfile p, String corpus, UUID id) {
                [status: 'READY', stale: false]
            }
        }
        def service = new SetSemanticSearchService(repository, indexes, new EmbeddingProviderRegistry([]), 1000)
        def readable = { String type, UUID id -> true } as BiPredicate<String, UUID>
        when:
        def whole = service.candidates(new SetSemanticSearchRequest(max: 20), 'Terminology', source, readable)
        def page1 = service.candidates(new SetSemanticSearchRequest(max: 1), 'Terminology', source, readable)
        def page2 = service.candidates(new SetSemanticSearchRequest(max: 1, offset: 1), 'Terminology', source, readable)
        then:
        whole.count == 2
        !whole.countIsExact
        whole.items*.rankingBasis == ['reciprocal-rank-fusion', 'reciprocal-rank-fusion']
        whole.items.every { !it.containsKey('similarity') && !it.containsKey('matches') }
        whole.items.every { it.evidence.size() == 2 }
        (page1.items + page2.items)*.id == whole.items*.id
        page1.count == page2.count
        limits.toSet() == [100].toSet()
        calls.toSet() == ['one:a', 'two:b'].toSet()
        when:
        def single = service.candidates(new SetSemanticSearchRequest(embeddingProfile: 'one'), 'Terminology', source, readable)
        then:
        single.items*.id == [first, second]
        single.items.first().similarity == 0.99D
        single.dimensions*.corpus == ['a']
        !single.items.first().evidence.first().index.containsKey('generation')
        single.items.first().evidence.first().index.coverageTargetMet == false
        single.items.first().evidence.first().index.generationIncomplete == false
        when:
        def detail = service.candidates(new SetSemanticSearchRequest(corpus: 'a', diagnostics: true), 'Terminology', source, readable)
        then:
        detail.items.first().containsKey('matches')
        detail.items.first().index.containsKey('generation')
    }

    void 'one contributing dimension retains cosine ranking and reports empty dimension reason (#reason)'() {
        given:
        String expectedReason = reason
        UUID source = UUID.randomUUID()
        UUID target = UUID.randomUUID()
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override EmbeddingProfile findProfileByName(String name) { new EmbeddingProfile(name: name, distanceMetric: 'cosine') }
            @Override List<Map<String, Object>> corpora(boolean internal) { [[name: 'a', enabled: true]] }
            @Override List<SetSemanticCentroid> setSemanticCentroids(EmbeddingProfile p, String corpus, UUID id, String type) {
                if (p.name == 'empty' && expectedReason == 'no_source_vectors') return []
                [new SetSemanticCentroid(setId: source, setDomainType: 'Terminology', vectorFamily: 'meaning', centroidKind: 'region', regionOrdinal: 1, mauroModelId: source)]
            }
            @Override List<SetSemanticCandidate> scopedSetSemanticCandidates(EmbeddingProfile p, String corpus, List<SetSemanticCentroid> vectors, int limit, List<UUID> eligible) {
                p.name == 'empty' ? [] : [candidate(target, 0.88D)]
            }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override List<Map<String, Object>> discoveryDimensions(String profile, String corpus) {
                [[profile: 'empty', corpus: 'a'], [profile: 'usable', corpus: 'a']]
            }
            @Override List<Map<String, Object>> destinations(EmbeddingProfile p, String corpus, UUID scope, List<String> types) {
                p.name == 'empty' && expectedReason == 'no_eligible_indexed_destinations' ? [] :
                    [[set_id: target, set_domain_type: 'Terminology', id: target, domain_type: 'Terminology', label: 'target']]
            }
            @Override UUID ownerModelId(UUID id, String type) { source }
            @Override Map<String, Object> state(EmbeddingProfile p, String corpus, UUID id) { [status: p.name, stale: false] }
        }
        def service = new SetSemanticSearchService(repository, indexes, new EmbeddingProviderRegistry([]), 1000)
        when:
        def result = service.candidates(new SetSemanticSearchRequest(withinModelId: UUID.randomUUID()), 'Terminology', source,
            { String type, UUID id -> true } as BiPredicate<String, UUID>)
        then:
        result.items.size() == 1
        result.items.first().similarity == 0.88D
        result.items.first().rankingBasis == 'best-vector'
        !result.items.first().containsKey('rankingScore')
        result.embeddingProfile == 'usable'
        result.source.index.status == 'usable'
        result.dimensions.first().emptyReason == reason
        result.dimensions.first().sourceAvailable == (reason != 'no_source_vectors')
        result.dimensions.last().emptyReason == null
        where:
        reason << ['no_source_vectors', 'no_eligible_indexed_destinations', 'no_readable_candidates_retrieved']
    }

    private static SetSemanticCandidate candidate(UUID id, double score) {
        new SetSemanticCandidate(targetSetId: id, targetSetDomainType: 'Terminology', targetMauroModelId: id,
            targetSetLabel: id.toString(), targetVectorFamily: 'meaning', sourceVectorFamily: 'meaning',
            sourceCentroidKind: 'region', sourceRegionOrdinal: 1, targetCentroidKind: 'region', targetRegionOrdinal: 1,
            distance: 1D - score, similarity: score,
            targetMetadata: [incomplete: false, stoppingReason: 'region-cap', coveredMemberCount: 16, representedMemberCount: 89])
    }
}
