package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.service.semantic.EmbeddingProfile

import java.util.function.BiPredicate

/** Candidate relevance only: no term mappings, equivalence scores or cardinality constraints. */
@CompileStatic
@Singleton
class SetSemanticSearchService {
    private final SemanticRepository repository
    private final SetSemanticIndexRepository indexRepository
    private final EmbeddingProviderRegistry providers
    private final int maxCentroidHitsPerVector

    SetSemanticSearchService(SemanticRepository repository, SetSemanticIndexRepository indexRepository,
                             EmbeddingProviderRegistry providers,
                             @Value('${chat.semantic.set-index.max-centroid-hits-per-vector:1000}') int maxCentroidHitsPerVector) {
        this.repository = repository
        this.indexRepository = indexRepository
        this.providers = providers
        if (maxCentroidHitsPerVector < 1 || maxCentroidHitsPerVector > 1000) {
            throw new IllegalArgumentException('max-centroid-hits-per-vector must be between 1 and 1000')
        }
        this.maxCentroidHitsPerVector = maxCentroidHitsPerVector
    }

    SetSemanticSearchResponse search(SetSemanticSearchRequest request, BiPredicate<String, UUID> readable) {
        discover(request, null, null, readable)
    }

    SetSemanticSearchResponse candidates(SetSemanticSearchRequest request, String type, UUID id, BiPredicate<String, UUID> readable) {
        discover(request, type, id, readable)
    }

    private List<Map<String, Object>> dimensions(SetSemanticSearchRequest request) {
        if (request == null || request.max == null || request.max < 1 || request.max > 100 || request.offset == null || request.offset < 0)
            throw new IllegalArgumentException('Require max between 1 and 100 and nonnegative offset')
        if (request.candidateLimit == null || request.candidateLimit < 1 || request.candidateLimit > 1000)
            throw new IllegalArgumentException('candidateLimit must be between 1 and 1000')
        if ((request.domainTypes ?: []).any { !(it in ['Terminology', 'CodeSet', 'DataType', 'DataElement', 'DataClass', 'DataModel']) })
            throw new IllegalArgumentException('Unsupported result domainTypes')
        if (request.embeddingProfile != null && !request.embeddingProfile.trim() || request.corpus != null && !request.corpus.trim())
            throw new IllegalArgumentException('Explicit dimensions must not be blank')
        if (request.embeddingProfile && repository.findProfileByName(request.embeddingProfile)?.distanceMetric != 'cosine') throw new IllegalArgumentException('An enabled cosine embedding profile is required')
        if (request.corpus && !repository.corpora().any { it.enabled == true && it.name == request.corpus }) throw new IllegalArgumentException('An enabled public corpus is required')
        if (request.embeddingProfile && request.corpus) return [[profile: request.embeddingProfile, corpus: request.corpus]] as List<Map<String, Object>>
        indexRepository.discoveryDimensions(request.embeddingProfile, request.corpus)
    }

    private static SetSemanticSearchRequest inDimension(SetSemanticSearchRequest request, Map<String, Object> dimension) {
        new SetSemanticSearchRequest(embeddingProfile: dimension.profile, corpus: dimension.corpus,
            query: request.query, withinModelId: request.withinModelId, domainTypes: request.domainTypes,
            max: request.max, offset: 0,
            includeIdentifiers: request.includeIdentifiers, diagnostics: request.diagnostics, candidateLimit: request.candidateLimit)
    }

    private SetSemanticSearchResponse discover(SetSemanticSearchRequest request, String type, UUID id, BiPredicate<String, UUID> readable) {
        List<Map<String, Object>> selected = dimensions(request)
        if (type == null && !request.query?.trim()) throw new IllegalArgumentException('query is required')
        Map<String, Map<String, Object>> merged = [:]
        List<Map<String, Object>> outcomes = []
        Map<String, Object> source = null
        List<Map<String, Object>> contributing = []
        Map<String, Object> contributingSource = null
        boolean exhausted = true
        boolean capped = false
        for (Map<String, Object> dimension : selected) {
            SetSemanticSearchRequest scoped = inDimension(request, dimension)
            SetSemanticSearchResponse response = type == null ? searchSingle(scoped, readable) : candidatesSingle(scoped, type, id, readable)
            if (response.items) {
                contributing.add(dimension)
                contributingSource = response.source
            }
            exhausted &= response.countIsExact == true
            capped |= response.candidateBudgetReached == true
            outcomes.add([embeddingProfile: dimension.profile, corpus: dimension.corpus,
                sourceAvailable: response.source?.searchable, retrievedCount: response.count, emptyReason: response.emptyReason,
                sourceVectorCount: request.diagnostics == true ? response.sourceVectorCount : null] as Map<String, Object>)
            if (source == null && response.source != null) source = new LinkedHashMap<>(response.source)
            else if (response.source?.searchable == true) source.put('searchable', true)
            int position = 0
            for (Map<String, Object> item : response.items) {
                position++
                String key = item.domainType.toString() + ':' + item.id
                if (!merged.containsKey(key)) {
                    Map<String, Object> entry = new LinkedHashMap<>(item)
                    entry.put('rankingScore', 0D)
                    entry.put('evidence', [])
                    merged.put(key, entry)
                }
                Map<String, Object> entry = merged.get(key)
                entry.put('rankingScore', ((Number) entry.rankingScore).doubleValue() + 1D / (60D + position))
                ((List) entry.evidence).add([embeddingProfile: dimension.profile, corpus: dimension.corpus,
                    similarity: item.similarity, rankingBasis: item.rankingBasis,
                    index: item.index, supportingSets: item.supportingSets, matches: request.diagnostics == true ? item.matches : null] as Map<String, Object>)
            }
        }
        List<Map<String, Object>> ranked = merged.values().toList().sort { a, b ->
            ((Double) b.rankingScore <=> (Double) a.rankingScore) ?:
                (a.domainType.toString() <=> b.domainType.toString()) ?: (a.id.toString() <=> b.id.toString())
        }
        List<Map<String, Object>> page = ranked.drop(request.offset).take(request.max)
        for (Map<String, Object> item : page) {
            if (contributing.size() > 1) {
                item.remove('similarity'); item.remove('overallSimilarity'); item.remove('bestVectorSimilarity')
                item.put('rankingBasis', 'reciprocal-rank-fusion')
                for (String key : ['index', 'supportingSets', 'matches', 'sourceRegionCount', 'retrievedSourceRegionCount', 'retrievedSourceRegionFraction', 'matchedSourceVectorCount', 'meaningEvidence', 'vectorFamily']) item.remove(key)
            } else item.remove('rankingScore')
            if (request.diagnostics != true) compact(item)
        }
        if (contributing.size() == 1 && contributingSource != null) source = new LinkedHashMap<>(contributingSource)
        if (source == null && type != null) source = [id: id, domainType: type, searchable: false] as Map<String, Object>
        if (source != null) {
            if (contributing.size() > 1 || contributing.isEmpty() && selected.size() > 1) source.remove('index')
            else if (request.diagnostics != true && source.index instanceof Map) source.put('index', qualitySummary((Map) source.index))
        }
        new SetSemanticSearchResponse(items: page, count: ranked.size(), countIsExact: exhausted, source: source,
            dimensions: outcomes, candidateBudgetReached: capped,
            embeddingProfile: contributing.size() == 1 ? contributing.first().profile.toString() : (selected.size() == 1 ? selected.first().profile.toString() : null),
            emptyReason: selected.isEmpty() ? 'no_established_index_dimensions' : null,
            centroidHitsPerVector: request.diagnostics == true ? Math.min(request.candidateLimit, maxCentroidHitsPerVector) : null)
    }

    private static Map<String, Object> qualitySummary(Map index) {
        Map generation = index.generation instanceof Map ? (Map) index.generation : [:]
        Map availability = index.inputAvailability instanceof Map ? (Map) index.inputAvailability : [:]
        [status: index.status, stale: index.stale, generationIncomplete: generation.incomplete,
         inputIncomplete: availability.incomplete, coverageTargetMet: generation.stoppingReason == null ? null : generation.stoppingReason == 'coverage-target',
         coveredMemberCount: generation.coveredMemberCount, representedMemberCount: generation.representedMemberCount] as Map<String, Object>
    }

    private static void compact(Map<String, Object> item) {
        for (String key : ['overallSimilarity', 'bestVectorSimilarity', 'sourceRegionCount', 'retrievedSourceRegionCount',
                          'retrievedSourceRegionFraction', 'matchedSourceVectorCount', 'matches', 'vectorFamily', 'meaningEvidence', 'supportingSets', 'index']) item.remove(key)
        for (Map evidence : (List<Map>) item.evidence) {
            evidence.remove('matches')
            if (evidence.index == null) evidence.remove('index')
            if (evidence.supportingSets == null) evidence.remove('supportingSets')
            if (evidence.index instanceof Map) evidence.put('index', qualitySummary((Map) evidence.index))
            if (evidence.supportingSets instanceof List) for (Map support : (List<Map>) evidence.supportingSets) {
                support.remove('bestVectorSimilarity'); support.remove('overallSimilarity')
                if (support.index instanceof Map) support.put('index', qualitySummary((Map) support.index))
            }
        }
    }

    EmbeddingProfile profile(SetSemanticSearchRequest request) {
        if (!request?.corpus?.trim()) throw new IllegalArgumentException('corpus is required for this operation')
        if (!request?.embeddingProfile?.trim()) throw new IllegalArgumentException('embeddingProfile is required')
        if (request.max == null || request.max < 1 || request.max > 100) throw new IllegalArgumentException('max must be between 1 and 100')
        if (request.offset == null || request.offset < 0) {
            throw new IllegalArgumentException('Require a nonnegative offset')
        }
        if ((request.domainTypes ?: []).any { !(it in ['Terminology', 'CodeSet', 'DataType', 'DataElement', 'DataClass', 'DataModel']) }) throw new IllegalArgumentException('Unsupported result domainTypes')
        if (!repository.corpora().any { it.name == request.corpus && it.enabled == true }) {
            throw new IllegalArgumentException('An enabled public corpus is required')
        }
        EmbeddingProfile profile = repository.findProfileByName(request.embeddingProfile)
        if (profile == null || profile.distanceMetric != 'cosine') throw new IllegalArgumentException('An enabled cosine embedding profile is required')
        profile
    }

    private SetSemanticSearchResponse searchSingle(SetSemanticSearchRequest request, BiPredicate<String, UUID> readable) {
        EmbeddingProfile profile = profile(request)
        if (!request.query?.trim()) throw new IllegalArgumentException('query is required')
        float[] vector = providers.providerFor(profile).embed(profile, [request.query.trim()]).first()
        if (!SetSemanticCentroidCalculator.isUsable(vector)) throw new IllegalArgumentException('The query has no usable embedding')
        List<SetSemanticCentroid> sources = [new SetSemanticCentroid(setId: new UUID(0L, 0L), setDomainType: 'Query',
            vectorFamily: 'meaning', centroidKind: 'query', embedding: SetSemanticCentroidCalculator.normalize(vector.clone() as float[]))]
        if (request.includeIdentifiers == true) sources.add(new SetSemanticCentroid(setId: new UUID(0L, 0L), setDomainType: 'Query',
            vectorFamily: 'identifier', centroidKind: 'query', embedding: sources.first().embedding))
        retrieve(profile, request, sources, readable)
    }

    private SetSemanticSearchResponse candidatesSingle(SetSemanticSearchRequest request, String domainType, UUID setId, BiPredicate<String, UUID> readable) {
        EmbeddingProfile profile = profile(request)
        Map<String, Object> resolved = domainType in ['DataElement', 'DataType'] ? indexRepository.resolveSource(domainType, setId) : ([id: setId, domainType: domainType] as Map<String, Object>)
        UUID resolvedId = (UUID) resolved.id
        String resolvedType = resolved.domainType.toString()
        List<SetSemanticCentroid> sources = repository.setSemanticCentroids(profile, request.corpus, resolvedId, resolvedType).findAll {
            request.includeIdentifiers == true || it.vectorFamily == 'meaning'
        }
        SetSemanticSearchResponse result = retrieve(profile, request, sources, readable, domainType, setId)
        SetSemanticCentroid primary = sources.find { it.vectorFamily == 'meaning' } ?: (sources ? sources.first() : null)
        UUID owner = primary != null ? primary.mauroModelId : indexRepository.ownerModelId(resolvedId, resolvedType)
        result.source = [id: setId, domainType: domainType, searchable: !sources.isEmpty()] as Map<String, Object>
        if (readable.test(resolvedType, resolvedId)) {
            result.source.put('set', resolved)
            result.source.put('index', quality(profile, request.corpus, owner, primary != null ? primary.metadata : [:],
                [resolvedType, resolvedId, primary?.vectorFamily ?: 'meaning'].join('|')))
        }
        result
    }

    private SetSemanticSearchResponse retrieve(EmbeddingProfile profile, SetSemanticSearchRequest request,
                                         List<SetSemanticCentroid> sources, BiPredicate<String, UUID> readable, String sourceType = null, UUID sourceId = null) {
        if (!sources) return new SetSemanticSearchResponse(items: [], count: 0, countIsExact: true, sourceVectorCount: 0, candidateBudgetReached: false, emptyReason: 'no_source_vectors')
        List<Map<String, Object>> destinations = request.withinModelId != null || request.domainTypes ? indexRepository.destinations(profile, request.corpus, request.withinModelId, request.domainTypes) : null
        if (destinations != null) destinations = destinations.findAll { !(it.id == sourceId && it.domain_type == sourceType) }
        List<UUID> eligible = destinations == null ? null : destinations.collect { (UUID) it.set_id }.unique()
        if (eligible != null && eligible.isEmpty()) return new SetSemanticSearchResponse(items: [], count: 0, countIsExact: true, sourceVectorCount: sources.size(), candidateBudgetReached: false, emptyReason: 'no_eligible_indexed_destinations')
        int window = Math.min(maxCentroidHitsPerVector, request.candidateLimit)
        List<Map<String, Object>> ranked = []
        List<SetSemanticCandidate> allowed = []
        Map<String, Boolean> access = [:]
        boolean budgetReached = false
        while (true) {
            List<SetSemanticCandidate> hits = destinations == null ? repository.searchSetSemanticCandidates(profile, request.corpus, sources, window) :
                repository.scopedSetSemanticCandidates(profile, request.corpus, sources, window, eligible)
            allowed = hits.findAll { SetSemanticCandidate hit ->
                if (destinations != null) return true
                String key = hit.targetSetDomainType + ':' + hit.targetSetId.toString()
                if (!access.containsKey(key)) access.put(key, readable.test(hit.targetSetDomainType, hit.targetSetId))
                access.get(key)
            }
            ranked = project(rank(allowed), destinations, readable)
            budgetReached = hits.groupBy { [it.sourceVectorFamily, it.sourceCentroidKind, it.sourceRegionOrdinal].join('|') }.values().any { it.size() >= window }
            break // The shortlist budget is independent of page size and offset.
        }
        if (sources.any { it.centroidKind == 'overall' && it.vectorFamily == 'meaning' } && allowed) {
            List<UUID> owners = allowed.collect { it.targetSetId }.unique()
            List<SetSemanticCandidate> overall = repository.overallSetSemanticCandidates(profile, request.corpus, sources, owners)
            allowed = allowed.findAll { !(it.sourceCentroidKind == 'overall' && it.targetCentroidKind == 'overall' && it.targetVectorFamily == 'meaning') } + overall
        }
        ranked = project(rank(allowed, sources), destinations, readable)
        List<Map<String, Object>> items = ranked
        for (Map<String, Object> item : items) {
            if (destinations == null) item.put('index', quality(profile, request.corpus, (UUID) item.remove('mauroModelId'),
                (Map<String, Object>) item.remove('metadata'), [item.domainType, item.id, item.vectorFamily].join('|')))
            else for (Map<String, Object> evidence : (List<Map<String, Object>>) item.supportingSets) {
                if (evidence.containsKey('id')) evidence.put('index', quality(profile, request.corpus,
                    (UUID) evidence.remove('mauroModelId'), (Map<String, Object>) evidence.remove('metadata'),
                    [evidence.domainType, evidence.id, evidence.remove('vectorFamily')].join('|')))
            }
        }
        new SetSemanticSearchResponse(items: items, count: ranked.size(), countIsExact: false,
            emptyReason: items.isEmpty() ? 'no_readable_candidates_retrieved' : null,
            sourceVectorCount: sources.size(), centroidHitsPerVector: window,
            candidateBudgetReached: budgetReached, embeddingProfile: profile.name)
    }

    private static List<Map<String, Object>> project(List<Map<String, Object>> ranked,
            List<Map<String, Object>> destinations, BiPredicate<String, UUID> readable) {
        if (destinations == null) return ranked
        Map<String, List<Map<String, Object>>> bySet = destinations.groupBy { it.set_domain_type.toString() + ':' + it.set_id }
        Map<String, Map<String, Object>> results = [:]
        for (Map<String, Object> candidate : ranked) {
            for (Map<String, Object> target : bySet.get(candidate.domainType.toString() + ':' + candidate.id) ?: []) {
                String type = target.domain_type.toString()
                UUID id = (UUID) target.id
                if (!readable.test(type, id)) continue
                String key = type + ':' + id
                if (!results.containsKey(key)) {
                    Map<String, Object> item = new LinkedHashMap<>(candidate)
                    item.remove('metadata')
                    item.remove('mauroModelId')
                    item.putAll([id: id, domainType: type, label: target.label, supportingSets: []])
                    results.put(key, item)
                }
                Map<String, Object> evidence = [similarity: candidate.similarity, overallSimilarity: candidate.overallSimilarity,
                    bestVectorSimilarity: candidate.bestVectorSimilarity, rankingBasis: candidate.rankingBasis] as Map<String, Object>
                if (readable.test(candidate.domainType.toString(), (UUID) candidate.id)) {
                    evidence.putAll([id: candidate.id, domainType: candidate.domainType, label: candidate.label,
                        mauroModelId: candidate.mauroModelId, metadata: candidate.metadata, vectorFamily: candidate.vectorFamily])
                }
                ((List) results.get(key).get('supportingSets')).add(evidence)
            }
        }
        results.values().toList().sort { Map<String, Object> a, Map<String, Object> b ->
            ((Boolean) b.meaningEvidence <=> (Boolean) a.meaningEvidence) ?:
                ((b.overallSimilarity != null) <=> (a.overallSimilarity != null)) ?:
                ((Double) b.similarity <=> (Double) a.similarity) ?:
                (a.domainType.toString() <=> b.domainType.toString()) ?: (a.id.toString() <=> b.id.toString())
        }
    }

    static List<Map<String, Object>> rank(List<SetSemanticCandidate> hits, List<SetSemanticCentroid> sources = []) {
        Map<String, List<SetSemanticCandidate>> groups = hits.groupBy {
            it.targetSetDomainType + ':' + it.targetSetId.toString()
        } as Map<String, List<SetSemanticCandidate>>
        List<Map<String, Object>> results = []
        groups.values().each { List<SetSemanticCandidate> group ->
            List<SetSemanticCandidate> sorted = group.sort(false) { a, b -> a.distance <=> b.distance }
            List<SetSemanticCandidate> meaning = sorted.findAll { it.targetVectorFamily == 'meaning' }
            SetSemanticCandidate best = meaning ? meaning.first() : sorted.first()
            SetSemanticCandidate overall = meaning.find { it.sourceCentroidKind == 'overall' && it.targetCentroidKind == 'overall' }
            int sourceRegions = sources.findAll { it.vectorFamily == 'meaning' && it.centroidKind == 'region' }.collect { it.regionOrdinal }.unique().size()
            int retrievedRegions = meaning.findAll { it.sourceCentroidKind == 'region' }.collect { it.sourceRegionOrdinal }.unique().size()
            results.add([overallSimilarity: overall?.similarity, bestVectorSimilarity: best.similarity,
                rankingBasis: overall != null ? 'overall' : 'best-vector',
                sourceRegionCount: sourceRegions, retrievedSourceRegionCount: retrievedRegions,
                retrievedSourceRegionFraction: sourceRegions > 0 ? retrievedRegions / (double) sourceRegions : null,
                id: best.targetSetId, domainType: best.targetSetDomainType, label: best.targetSetLabel,
                similarity: overall != null ? overall.similarity : best.similarity, vectorFamily: best.targetVectorFamily, meaningEvidence: !meaning.isEmpty(), mauroModelId: best.targetMauroModelId,
                metadata: best.targetMetadata, matchedSourceVectorCount: group.collect {
                    [it.sourceVectorFamily, it.sourceCentroidKind, it.sourceRegionOrdinal].join('|')
                }.unique().size(), matches: (meaning + sorted.findAll { it.targetVectorFamily != 'meaning' }).take(5).collect { SetSemanticCandidate hit ->
                    [sourceKind: hit.sourceCentroidKind, sourceRegion: hit.sourceCentroidKind == 'region' ? hit.sourceRegionOrdinal : null,
                     targetKind: hit.targetCentroidKind, targetRegion: hit.targetCentroidKind == 'region' ? hit.targetRegionOrdinal : null,
                     vectorFamily: hit.targetVectorFamily, similarity: hit.similarity]
                }] as Map<String, Object>)
        }
        results.sort { Map<String, Object> a, Map<String, Object> b ->
            ((Boolean) b.meaningEvidence <=> (Boolean) a.meaningEvidence) ?:
                ((b.overallSimilarity != null) <=> (a.overallSimilarity != null)) ?:
                ((Double) b.similarity <=> (Double) a.similarity) ?:
                (a.domainType.toString() <=> b.domainType.toString()) ?: (a.id.toString() <=> b.id.toString())
        }
    }

    private Map<String, Object> quality(EmbeddingProfile profile, String corpus, UUID owner, Map<String, Object> generation, String setKey = null) {
        Map<String, Object> state = indexRepository.state(profile, corpus, owner)
        Map<String, Object> availability = [:]
        if (setKey != null && state.metadata instanceof Map) {
            Map<String, Object> published = (Map<String, Object>) state.metadata
            if (published.sets instanceof Map) availability = (Map<String, Object>) ((Map) published.sets).get(setKey) ?: [:]
        }
        if (!generation) generation = availability
        [status: state.status, stale: state.stale, lastError: state.last_error,
         requestedAt: state.requested_at, completedAt: state.completed_at,
         generation: generation, inputAvailability: availability] as Map<String, Object>
    }
}
