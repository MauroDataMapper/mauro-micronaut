package org.maurodata.plugin.chat.semantic

import org.maurodata.service.semantic.EmbeddingProfile
import spock.lang.Specification

import java.util.function.BiPredicate

class SetSemanticSearchServiceSpec extends Specification {
    void 'one matching region of a large target makes it a candidate with no size penalty'() {
        given:
        UUID large = UUID.randomUUID()
        UUID other = UUID.randomUUID()
        def hits = [hit(large, 0.98D, 100000, 'region'), hit(other, 0.8D, 2, 'region')]
        when:
        def ranked = SetSemanticSearchService.rank(hits)
        then:
        ranked*.id == [large, other]
        ranked.first().similarity == 0.98D
        ranked.first().matches.size() == 1
    }

    void 'overall context ranks smoking above an unrelated set sharing a perfect status region'() {
        given:
        UUID smoking = UUID.randomUUID()
        UUID gender = UUID.randomUUID()
        def smokingRegion = hit(smoking, 1D, 4, 'region')
        def genderRegion = hit(gender, 1D, 6, 'region')
        smokingRegion.sourceCentroidKind = genderRegion.sourceCentroidKind = 'region'
        smokingRegion.sourceRegionOrdinal = genderRegion.sourceRegionOrdinal = 1
        def sources = (1..3).collect { new SetSemanticCentroid(vectorFamily: 'meaning', centroidKind: 'region', regionOrdinal: it) }
        when:
        def ranked = SetSemanticSearchService.rank([
            genderRegion, hit(gender, 0.8705D, 6, 'overall'),
            smokingRegion, hit(smoking, 0.9829D, 4, 'overall')], sources)
        then:
        ranked*.id == [smoking, gender]
        ranked*.similarity == [0.9829D, 0.8705D]
        ranked*.bestVectorSimilarity == [1D, 1D]
        ranked.first().sourceRegionCount == 3
        ranked.first().retrievedSourceRegionCount == 1
        ranked.first().retrievedSourceRegionFraction == 1D / 3D
    }

    void 'meaning ranks ahead of optional identifier-only evidence'() {
        given:
        def meaning = hit(UUID.randomUUID(), 0.8D, 100, 'region')
        def identifier = hit(UUID.randomUUID(), 0.99D, 1, 'overall')
        identifier.targetVectorFamily = 'identifier'
        expect:
        SetSemanticSearchService.rank([identifier, meaning])*.id == [meaning.targetSetId, identifier.targetSetId]
    }

    void 'fixed retrieval budget groups readable owners and scores overall context'() {
        given:
        def profile = new EmbeddingProfile(id: UUID.randomUUID(), name: 'test', distanceMetric: 'cosine')
        UUID source = UUID.randomUUID()
        UUID denied = UUID.randomUUID()
        UUID first = UUID.randomUUID()
        UUID second = UUID.randomUUID()
        List<Integer> windows = []
        def repository = new SemanticRepository(null, 1, 1, 1, 1) {
            @Override EmbeddingProfile findProfileByName(String name) { profile }
            @Override List<Map<String, Object>> corpora(boolean includeInternal) { [[name: 'catalogue-items', enabled: true]] }
            @Override List<SetSemanticCentroid> setSemanticCentroids(EmbeddingProfile p, String corpus, UUID id, String type) {
                [new SetSemanticCentroid(setId: source, setDomainType: 'CodeSet', vectorFamily: 'meaning', centroidKind: 'overall', embedding: [1F, 0F] as float[])]
            }
            @Override List<SetSemanticCandidate> overallSetSemanticCandidates(EmbeddingProfile p, String corpus, List<SetSemanticCentroid> vectors, List<UUID> owners) {
                assert owners.toSet() == [first, second].toSet()
                [hit(first, 0.4D, 1, 'overall'), hit(second, 0.95D, 1, 'overall')]
            }
            @Override List<SetSemanticCandidate> searchSetSemanticCandidates(EmbeddingProfile p, String corpus, List<SetSemanticCentroid> vectors, int window) {
                windows.add(window)
                def hits = [hit(denied, 1D, 1, 'overall'), hit(first, 0.9D, 1, 'overall')]
                if (window > 32) hits.add(hit(second, 0.8D, 1, 'region'))
                hits
            }
        }
        def indexes = new SetSemanticIndexRepository(null) {
            @Override Map<String, Object> state(EmbeddingProfile p, String corpus, UUID owner) { [status: 'READY', stale: false] }
        }
        def service = new SetSemanticSearchService(repository, indexes, new EmbeddingProviderRegistry([]), 128)
        when:
        def result = service.candidates(new SetSemanticSearchRequest(diagnostics: true, corpus: 'catalogue-items', embeddingProfile: 'test', max: 2), 'CodeSet', source,
            { String type, UUID id -> id != denied } as BiPredicate<String, UUID>)
        then:
        windows == [100]
        result.items*.id == [second, first]
        result.items*.overallSimilarity == [0.95D, 0.4D]
    }

    void 'projection aggregates supporting sets using the strongest overall score'() {
        given:
        UUID first = UUID.randomUUID()
        UUID second = UUID.randomUUID()
        UUID model = UUID.randomUUID()
        def ranked = SetSemanticSearchService.rank([hit(first, 0.95D, 1, 'overall'), hit(second, 0.7D, 1, 'overall')])
        def destinations = [first, second].collect {
            [set_id: it, set_domain_type: 'Terminology', id: model, domain_type: 'DataModel', label: 'destination']
        }
        when:
        def results = SetSemanticSearchService.project(ranked, destinations, { String t, UUID id -> true } as BiPredicate<String, UUID>)
        then:
        results.size() == 1
        results.first().id == model
        results.first().similarity == 0.95D
        results.first().supportingSets*.similarity == [0.95D, 0.7D]
    }

    private static SetSemanticCandidate hit(UUID id, double similarity, int size, String kind) {
        new SetSemanticCandidate(targetSetId: id, targetSetDomainType: 'Terminology', targetSetLabel: id.toString(),
            targetMauroModelId: id, targetVectorFamily: 'meaning', sourceVectorFamily: 'meaning', sourceCentroidKind: 'overall',
            targetCentroidKind: kind, targetMemberCount: size, similarity: similarity, distance: 1D - similarity)
    }
}
