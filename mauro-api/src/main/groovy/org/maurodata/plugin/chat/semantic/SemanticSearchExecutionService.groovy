package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticChunkMatchDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.semantic.EmbeddingProfile
import org.maurodata.service.semantic.EmbeddingProvider




import org.maurodata.web.ListResponse

import java.security.MessageDigest
import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
class SemanticSearchExecutionService implements SemanticSearchService {

    private final SemanticRepository semanticRepository
    private final EmbeddingProviderRegistry embeddingProviderRegistry
    private final SemanticIndexingService semanticIndexingService
    private final AccessControlService accessControlService
    private final PathRepository pathRepository
    private final int accessFilterFetchMultiplier
    private final int minimumCandidateWindow
    private final int maximumCandidateWindow
    private final int deepCandidateWindow
    private final boolean includeContext
    private final int queryEmbeddingCacheSize
    private final Map<String, float[]> queryEmbeddingCache

    SemanticSearchExecutionService(SemanticRepository semanticRepository,
                                   EmbeddingProviderRegistry embeddingProviderRegistry,
                                   SemanticIndexingService semanticIndexingService,
                                   AccessControlService accessControlService,
                                   PathRepository pathRepository,
                                   @Value('${chat.semantic.search.access-filter-fetch-multiplier:100}') Integer accessFilterFetchMultiplier,
                                   @Value('${chat.semantic.search.minimum-candidate-window:400}') Integer minimumCandidateWindow,
                                   @Value('${chat.semantic.search.maximum-candidate-window:600}') Integer maximumCandidateWindow,
                                   @Value('${chat.semantic.search.deep-candidate-window:1600}') Integer deepCandidateWindow,
                                   @Value('${chat.semantic.search.include-context:false}') Boolean includeContext,
                                   @Value('${chat.semantic.search.query-embedding-cache-size:1000}') Integer queryEmbeddingCacheSize) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
        this.semanticIndexingService = semanticIndexingService
        this.accessControlService = accessControlService
        this.pathRepository = pathRepository
        this.accessFilterFetchMultiplier = Math.max(accessFilterFetchMultiplier ?: 100, 1)
        this.minimumCandidateWindow = Math.max(minimumCandidateWindow ?: 1000, 1)
        this.maximumCandidateWindow = Math.max(maximumCandidateWindow ?: 2000, this.minimumCandidateWindow)
        this.deepCandidateWindow = Math.max(deepCandidateWindow ?: 2000, this.maximumCandidateWindow)
        this.includeContext = Boolean.TRUE.equals(includeContext)
        this.queryEmbeddingCacheSize = Math.max(queryEmbeddingCacheSize ?: 1000, 0)
        this.queryEmbeddingCache = Collections.synchronizedMap(new LinkedHashMap<String, float[]>(128, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                SemanticSearchExecutionService.this.queryEmbeddingCacheSize > 0 &&
                    size() > SemanticSearchExecutionService.this.queryEmbeddingCacheSize
            }
        })
    }

    @Connectable
    @Override
    ListResponse<SemanticSearchResultsDTO> executeSearch(SemanticSearchRequestDTO request,
                                                         BiFunction<String, UUID, AdministeredItem> itemLookup) {
        SemanticSearchRequestDTO safeRequest = request ?: new SemanticSearchRequestDTO()
        String queryText = queryText(safeRequest)
        if (queryText == null || queryText.trim().isEmpty()) {
            return ListResponse.from([] as List<SemanticSearchResultsDTO>, safeRequest)
        }
        long totalStart = System.currentTimeMillis()

        String indexName = safeRequest.indexName ?: 'catalogue-items-default'
        if (Boolean.TRUE.equals(safeRequest.rebuildIfEmpty) && !semanticIndexingService.hasEmbeddings(indexName)) {
            semanticIndexingService.rebuildCatalogueIndex(indexName, safeRequest.corpus ?: 'catalogue-items', safeRequest.domainTypes, safeRequest.withinModelId)
        }

        List<String> corpusNames = searchCorpora(safeRequest)
        long corpusResolvedAt = System.currentTimeMillis()
        if (corpusNames.isEmpty()) {
            return ListResponse.from([] as List<SemanticSearchResultsDTO>, safeRequest)
        }
        Map<String, List<EmbeddingProfile>> profilesByCorpus = profilesByCorpus(safeRequest, corpusNames)
        long profilesResolvedAt = System.currentTimeMillis()
        int requestedTopN = Math.max(1, safeRequest.topN ?: 50)
        int topM = Math.max(1, Math.min(safeRequest.topM ?: safeRequest.max ?: 10, 100))
        int topN = candidateWindow(requestedTopN, topM, safeRequest.domainTypes, safeRequest.deepSearch == true)
        List<SemanticCandidate> candidates = new ArrayList<SemanticCandidate>()
        Map<String, Integer> candidateCounts = new LinkedHashMap<String, Integer>()
        long searchStart = System.currentTimeMillis()
        long vectorCatalogueMillis = 0L
        long vectorContextMillis = 0L
        Map<EmbeddingProfile, float[]> queryEmbeddings = new LinkedHashMap<EmbeddingProfile, float[]>()
        Map<String, String> queryEmbeddingFingerprints = new LinkedHashMap<String, String>()
        for (String corpusName : corpusNames) {
            for (EmbeddingProfile profile : profilesByCorpus.get(corpusName) ?: Collections.<EmbeddingProfile>emptyList()) {
                float[] queryEmbedding = queryEmbeddings.get(profile)
                if (queryEmbedding == null) {
                    queryEmbedding = queryEmbeddingFor(profile, queryText)
                    queryEmbeddings.put(profile, queryEmbedding)
                    queryEmbeddingFingerprints.put(profile.name, vectorFingerprint(queryEmbedding))
                }
                long vectorCatalogueStart = System.currentTimeMillis()
                List<SemanticCandidate> catalogueCandidates = semanticRepository.search(profile, queryEmbedding, corpusName, safeRequest.domainTypes, safeRequest.withinModelId, topN, 'catalogue')
                vectorCatalogueMillis += System.currentTimeMillis() - vectorCatalogueStart
                candidateCounts.put("${corpusName}:${profile.name}:catalogue".toString(), catalogueCandidates.size())
                candidates.addAll(catalogueCandidates)
                if (includeContext) {
                    long vectorContextStart = System.currentTimeMillis()
                    List<SemanticCandidate> contextCandidates = semanticRepository.search(profile, queryEmbedding, corpusName, safeRequest.domainTypes, safeRequest.withinModelId, topN, 'context')
                    vectorContextMillis += System.currentTimeMillis() - vectorContextStart
                    candidateCounts.put("${corpusName}:${profile.name}:context".toString(), contextCandidates.size())
                    candidates.addAll(contextCandidates)
                }
            }
        }

        long rerankStart = System.currentTimeMillis()
        List<SemanticSearchResultsDTO> ranked = rerank(candidates, safeRequest.includeChunks != false)
        long rerankMillis = System.currentTimeMillis() - rerankStart
        long accessFilterStart = System.currentTimeMillis()
        FilteredSemanticResults filtered = filterReadable(ranked, itemLookup)
        List<SemanticSearchResultsDTO> readable = filtered.readable
        int unreadableCount = filtered.unreadableCount
        long accessFilterMillis = System.currentTimeMillis() - accessFilterStart
        if (readable.isEmpty()) {
            log.info(
                'Semantic search returned no readable results query="{}" domainTypes={} profiles={} topN={} candidateCounts={} ranked={} unreadable={}',
                queryText,
                safeRequest.domainTypes ?: [],
                profileNames(profilesByCorpus),
                topN,
                candidateCounts,
                ranked.size(),
                unreadableCount
            )
        }

        int offset = safeRequest.offset ?: 0
        int max = safeRequest.max != null && safeRequest.max > 0 ? safeRequest.max : topM
        List<SemanticSearchResultsDTO> page = readable.drop(offset).take(max)
        log.info(
            'Semantic search timing query="{}" domainTypes={} profiles={} topN={} deepSearch={} candidateCounts={} ranked={} readable={} unreadable={} returned={} queryEmbeddings={} returnedKeys={} timingsMs={corpusResolve={}, profileResolve={}, vectorCatalogue={}, vectorContext={}, rerank={}, accessFilter={}, total={}}',
            queryText,
            safeRequest.domainTypes ?: [],
            profileNames(profilesByCorpus),
            topN,
            safeRequest.deepSearch == true,
            candidateCounts,
            ranked.size(),
            readable.size(),
            unreadableCount,
            page.size(),
            queryEmbeddingFingerprints,
            resultKeys(page),
            corpusResolvedAt - totalStart,
            profilesResolvedAt - corpusResolvedAt,
            vectorCatalogueMillis,
            vectorContextMillis,
            rerankMillis,
            accessFilterMillis,
            System.currentTimeMillis() - totalStart
        )
        new ListResponse<SemanticSearchResultsDTO>(
            count: readable.size(),
            items: page
        )
    }

    @Connectable
    @Override
    SemanticSearchAvailability availability(String indexName) {
        availability(indexName, null)
    }

    @Connectable
    @Override
    SemanticSearchAvailability availability(String indexName, UUID mauroModelId) {
        try {
            String requestedCorpus = semanticRepository.apiCorpusVisible(indexName) ? indexName : null
            List<String> corpusNames = mauroModelId == null ?
                (requestedCorpus == null ? semanticRepository.apiCorpusNames() : [requestedCorpus] as List<String>) :
                semanticRepository.apiCorpusNamesForModelIndex(mauroModelId, requestedCorpus)
            List<EmbeddingProfile> profiles = distinctProfiles(profilesByCorpus(new SemanticSearchRequestDTO(corpus: requestedCorpus, withinModelId: mauroModelId), corpusNames))
            if (profiles.isEmpty() && requestedCorpus == null && indexName != null && !indexName.trim().isEmpty()) {
                String safeIndexName = indexName ?: 'catalogue-items-default'
                if (!semanticIndexingService.hasEmbeddings(safeIndexName, mauroModelId)) {
                    String scope = mauroModelId == null ? safeIndexName : "${safeIndexName} scoped to ${mauroModelId}".toString()
                    return SemanticSearchAvailability.unavailable("no semantic embeddings are available for ${scope}".toString())
                }
                profiles = semanticRepository.profilesForIndex(safeIndexName)
            }
            if (profiles.isEmpty()) {
                return SemanticSearchAvailability.unavailable('no enabled semantic profiles are available for the requested corpus/model scope')
            }
            boolean meaningful = profiles.any {EmbeddingProfile profile -> meaningfulSemanticProfile(profile)}
            meaningful ?
                SemanticSearchAvailability.available() :
                SemanticSearchAvailability.unavailable('only test-only semantic profiles are active')
        } catch (Exception e) {
            log.debug('Semantic search is not available', e)
            SemanticSearchAvailability.unavailable(e.message ?: e.class.simpleName)
        }
    }

    @Connectable
    @Override
    List<SearchResultsDTO> projectResults(List<SearchResultsDTO> sourceItems,
                                          List<String> targetDomainTypes) {
        semanticRepository.projectSearchResults(sourceItems, targetDomainTypes)
    }

    private Map<String, List<EmbeddingProfile>> profilesByCorpus(SemanticSearchRequestDTO request, List<String> corpusNames) {
        Map<String, List<EmbeddingProfile>> byCorpus = new LinkedHashMap<String, List<EmbeddingProfile>>()
        if (corpusNames == null || corpusNames.isEmpty()) {
            return byCorpus
        }
        if (request.embeddingProfiles != null && !request.embeddingProfiles.isEmpty()) {
            List<EmbeddingProfile> profiles = request.embeddingProfiles.collect {String name -> semanticRepository.findProfileByName(name)}
                .findAll {EmbeddingProfile profile -> profile != null} as List<EmbeddingProfile>
            corpusNames.each {String corpusName -> byCorpus.put(corpusName, profiles)}
            return byCorpus
        }
        if (request.withinModelId != null) {
            Map<String, List<EmbeddingProfile>> profilesByModelCorpus =
                semanticRepository.profilesForModelIndexCorpora(request.withinModelId, corpusNames)
            for (String corpusName : corpusNames) {
                byCorpus.put(corpusName, profilesByModelCorpus.get(corpusName) ?: Collections.<EmbeddingProfile>emptyList())
            }
            return byCorpus
        }
        List<EmbeddingProfile> profiles = semanticRepository.profilesForCorpora(corpusNames)
        corpusNames.each {String corpusName -> byCorpus.put(corpusName, profiles)}
        byCorpus
    }

    private List<String> searchCorpora(SemanticSearchRequestDTO request) {
        String requestedCorpus = request.corpus == null || request.corpus.trim().isEmpty() ? null : request.corpus.trim()
        if (request.withinModelId != null) {
            return semanticRepository.apiCorpusNamesForModelIndex(request.withinModelId, requestedCorpus)
        }
        if (requestedCorpus != null) {
            return semanticRepository.apiCorpusVisible(requestedCorpus) ? [requestedCorpus] as List<String> : Collections.<String>emptyList()
        }
        semanticRepository.apiCorpusNames()
    }

    private static List<String> profileNames(Map<String, List<EmbeddingProfile>> profilesByCorpus) {
        distinctProfiles(profilesByCorpus).collect {EmbeddingProfile profile -> profile.name} as List<String>
    }

    private static List<EmbeddingProfile> distinctProfiles(Map<String, List<EmbeddingProfile>> profilesByCorpus) {
        Map<String, EmbeddingProfile> byName = new LinkedHashMap<String, EmbeddingProfile>()
        profilesByCorpus.values().each {List<EmbeddingProfile> profiles ->
            profiles.each {EmbeddingProfile profile ->
                if (profile != null && !byName.containsKey(profile.name)) {
                    byName.put(profile.name, profile)
                }
            }
        }
        new ArrayList<EmbeddingProfile>(byName.values())
    }

    private static String queryText(SemanticSearchRequestDTO request) {
        request.query ?: request.searchTerm
    }

    private float[] queryEmbeddingFor(EmbeddingProfile profile, String queryText) {
        String key = queryEmbeddingCacheKey(profile, queryText)
        if (queryEmbeddingCacheSize > 0) {
            float[] cached = queryEmbeddingCache.get(key)
            if (cached != null) {
                return cached
            }
        }
        EmbeddingProvider provider = embeddingProviderRegistry.providerFor(profile)
        float[] embedding = provider.embed(profile, [queryText] as List<String>).first()
        if (queryEmbeddingCacheSize > 0) {
            queryEmbeddingCache.put(key, embedding)
        }
        embedding
    }

    private static String queryEmbeddingCacheKey(EmbeddingProfile profile, String queryText) {
        [
            profile.id?.toString() ?: '',
            profile.provider ?: '',
            profile.embeddingModel ?: '',
            String.valueOf(profile.dimension ?: 0),
            queryText ?: ''
        ].join('|')
    }

    private static String vectorFingerprint(float[] vector) {
        if (vector == null) {
            return ''
        }
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        for (float value : vector) {
            int bits = Float.floatToIntBits(value)
            digest.update((byte) ((bits >>> 24) & 0xff))
            digest.update((byte) ((bits >>> 16) & 0xff))
            digest.update((byte) ((bits >>> 8) & 0xff))
            digest.update((byte) (bits & 0xff))
        }
        byte[] hash = digest.digest()
        StringBuilder builder = new StringBuilder(16)
        for (int i = 0; i < Math.min(hash.length, 8); i++) {
            builder.append(String.format('%02x', hash[i] & 0xff))
        }
        builder.toString()
    }

    private static List<String> resultKeys(List<SemanticSearchResultsDTO> results) {
        (results ?: Collections.<SemanticSearchResultsDTO>emptyList()).collect {SemanticSearchResultsDTO result ->
            "${result.domainType}:${result.id}".toString()
        } as List<String>
    }

    private static boolean meaningfulSemanticProfile(EmbeddingProfile profile) {
        if (profile == null) {
            return false
        }
        String provider = profile.provider ?: ''
        String name = profile.name ?: ''
        !(provider.equalsIgnoreCase('test') || name.startsWith('test-'))
    }

    private static boolean projectsToBroaderArtefact(List<String> domainTypes) {
        if (domainTypes == null || domainTypes.isEmpty()) {
            return false
        }
        true
    }

    private int candidateWindow(int requestedTopN, int topM, List<String> domainTypes, boolean deepSearch) {
        int accessFilteredWindow = Math.max(requestedTopN, topM * accessFilterFetchMultiplier)
        int projectedWindow = projectsToBroaderArtefact(domainTypes) ? accessFilteredWindow * 5 : accessFilteredWindow
        int maximumWindow = deepSearch ? deepCandidateWindow : maximumCandidateWindow
        Math.min(Math.max(projectedWindow, minimumCandidateWindow), maximumWindow)
    }

    private FilteredSemanticResults filterReadable(List<SemanticSearchResultsDTO> ranked,
                                                   BiFunction<String, UUID, AdministeredItem> itemLookup) {
        int unreadableCount = 0
        List<SemanticSearchResultsDTO> readable = new ArrayList<SemanticSearchResultsDTO>()
        for (SemanticSearchResultsDTO result : ranked) {
            AdministeredItem item = itemLookup.apply(result.domainType, result.id)
            if (!accessControlService.canDoRole(Role.READER, item)) {
                unreadableCount++
                continue
            }
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
            result.classifiers = item.classifiers
            readable.add(result)
        }
        new FilteredSemanticResults(readable: readable, unreadableCount: unreadableCount)
    }

    private static List<SemanticSearchResultsDTO> rerank(List<SemanticCandidate> candidates, boolean includeChunks) {
        Map<String, List<SemanticCandidate>> grouped = candidates.groupBy {SemanticCandidate candidate ->
            "${resultDomainType(candidate)}:${resultId(candidate)}".toString()
        } as Map<String, List<SemanticCandidate>>
        List<SemanticSearchResultsDTO> results = new ArrayList<SemanticSearchResultsDTO>()
        for (List<SemanticCandidate> group : grouped.values()) {
            List<SemanticCandidate> sorted = group.sort {SemanticCandidate left, SemanticCandidate right ->
                ((right.similarity ?: 0D) <=> (left.similarity ?: 0D)) ?:
                    (candidateStableKey(left) <=> candidateStableKey(right))
            } as List<SemanticCandidate>
            SemanticCandidate best = sorted.first()
            double semanticScore = sorted.collect {SemanticCandidate candidate -> candidate.similarity ?: 0D}.max() ?: 0D
            double weightedScore = sorted.collect {SemanticCandidate candidate -> weightedSimilarity(candidate)}.max() ?: 0D
            double evidenceScore = ((Double) sorted.take(5).collect {SemanticCandidate candidate -> weightedSimilarity(candidate)}.sum(0D)) * 0.02D
            double rerankScore = weightedScore + evidenceScore + directTargetScore(sorted) + projectedSourceSupportScore(sorted) + repeatedEvidenceScore(sorted)
            SemanticSearchResultsDTO dto = new SemanticSearchResultsDTO(
                id: resultId(best),
                domainType: resultDomainType(best),
                label: resultLabel(best),
                description: resultDescription(best),
                dateCreated: best.dateCreated,
                lastUpdated: best.lastUpdated,
                semanticScore: semanticScore,
                rerankScore: rerankScore,
                matchedChunkCount: sorted.size(),
                embeddingProfiles: sorted.collect {SemanticCandidate candidate -> candidate.embeddingProfile}.unique() as List<String>,
                evidence: evidence(sorted),
                evidenceDetails: evidenceDetails(sorted)
            )
            if (includeChunks) {
                dto.chunks = sorted.take(5).collect {SemanticCandidate candidate ->
                    double significanceWeight = chunkKindWeight(candidate.chunkKind)
                    new SemanticChunkMatchDTO(
                        chunkId: candidate.chunkId,
                        chunkKind: candidate.chunkKind,
                        chunkGroup: candidate.chunkGroup,
                        chunkOrdinal: candidate.chunkOrdinal,
                        matchedSourceDomainType: candidate.sourceDomainType,
                        matchedSourceLabel: candidate.sourceLabel,
                        sourceText: candidate.sourceText,
                        embeddingProfile: candidate.embeddingProfile,
                        significanceWeight: significanceWeight,
                        weightedSimilarity: weightedSimilarity(candidate),
                        distance: candidate.distance,
                        similarity: candidate.similarity
                    )
                } as List<SemanticChunkMatchDTO>
            }
            results.add(dto)
        }
        results.sort {SemanticSearchResultsDTO left, SemanticSearchResultsDTO right ->
            ((right.rerankScore ?: 0D) <=> (left.rerankScore ?: 0D)) ?:
                (resultStableKey(left) <=> resultStableKey(right))
        } as List<SemanticSearchResultsDTO>
    }

    private static String candidateStableKey(SemanticCandidate candidate) {
        [
            resultDomainType(candidate) ?: '',
            resultLabel(candidate) ?: '',
            resultId(candidate)?.toString() ?: '',
            candidate.sourceDomainType ?: '',
            candidate.sourceLabel ?: '',
            candidate.sourceId?.toString() ?: '',
            candidate.chunkKind ?: '',
            String.format(Locale.ROOT, '%08d', candidate.chunkOrdinal ?: 0),
            candidate.chunkId?.toString() ?: ''
        ].join('|')
    }

    private static String resultStableKey(SemanticSearchResultsDTO result) {
        [
            result.domainType ?: '',
            result.label ?: '',
            result.id?.toString() ?: ''
        ].join('|')
    }

    static double chunkKindWeight(String chunkKind) {
        switch (chunkKind) {
            case 'label':
                return 1.35D
            case 'label-identifier':
                return 1.30D
            case 'label-phrase':
                return 1.25D
            case 'summary':
                return 1.10D
            case 'term-definition':
            case 'enumeration-key':
            case 'enumeration-value':
                return 1.05D
            case 'description-section':
            case 'description':
                return 1.00D
            case 'enumeration-category':
                return 0.95D
            case 'classification':
                return 0.70D
            case 'annotation':
                return 0.75D
            default:
                if (chunkKind != null && chunkKind.startsWith('semantic-link-')) {
                    return 0.80D
                }
                return 0.85D
        }
    }

    private static double weightedSimilarity(SemanticCandidate candidate) {
        (candidate.similarity ?: 0D) *
            retrievalModeWeight(candidate.embeddingProfile) *
            chunkKindWeight(candidate.chunkKind) *
            chunkGroupWeight(candidate.chunkGroup) *
            domainTypeWeight(resultDomainType(candidate)) *
            relationDistanceWeight(candidate.relationDistance)
    }

    static double retrievalModeWeight(String embeddingProfile) {
        embeddingProfile == 'lexical' ? 0.80D : 1.00D
    }

    static double chunkGroupWeight(String chunkGroup) {
        chunkGroup == 'context' ? 0.70D : 1.00D
    }

    static double directTargetScore(List<SemanticCandidate> candidates) {
        if (!candidates) {
            return 0D
        }
        double directBest = candidates.findAll {SemanticCandidate candidate -> isDirectTargetMatch(candidate)}
            .collect {SemanticCandidate candidate -> weightedSimilarity(candidate)}
            .max() ?: 0D
        directBest * 0.08D
    }

    static double projectedSourceSupportScore(List<SemanticCandidate> candidates) {
        int uniqueSources = uniqueSourceCount(candidates)
        uniqueSources <= 1 ? 0D : Math.min((uniqueSources - 1) * 0.015D, 0.12D)
    }

    static double repeatedEvidenceScore(List<SemanticCandidate> candidates) {
        candidates ? Math.min(Math.max(candidates.size() - 1, 0) * 0.003D, 0.06D) : 0D
    }

    private static boolean isDirectTargetMatch(SemanticCandidate candidate) {
        if (candidate == null) {
            return false
        }
        resultId(candidate) == candidate.sourceId && resultDomainType(candidate) == candidate.sourceDomainType
    }

    private static int uniqueSourceCount(List<SemanticCandidate> candidates) {
        if (!candidates) {
            return 0
        }
        candidates.collect {SemanticCandidate candidate ->
            "${candidate.sourceDomainType}:${candidate.sourceId}".toString()
        }.unique().size()
    }

    static double domainTypeWeight(String domainType) {
        switch (domainType) {
            case 'DataModel':
                return 1.20D
            case 'DataClass':
                return 1.10D
            case 'DataElement':
                return 1.00D
            case 'DataType':
            case 'EnumerationType':
                return 0.90D
            case 'EnumerationValue':
                return 0.80D
            default:
                return 0.95D
        }
    }

    static double relationDistanceWeight(Integer relationDistance) {
        int distance = Math.max(relationDistance ?: 0, 0)
        1D / (1D + (0.08D * distance))
    }

    private static List<String> evidence(List<SemanticCandidate> sorted) {
        sorted.take(5).collect {SemanticCandidate candidate ->
            "${matchText(candidate)}: similarity=${round(candidate.similarity)}, weighted=${round(weightedSimilarity(candidate))}".toString()
        } as List<String>
    }

    private static List<Map<String, Object>> evidenceDetails(List<SemanticCandidate> sorted) {
        sorted.take(5).collect {SemanticCandidate candidate ->
            [
                match     : matchText(candidate),
                confidence: [
                    similarity: roundedDouble(candidate.similarity),
                    weighted  : roundedDouble(weightedSimilarity(candidate))
                ] as Map<String, Object>,
                source    : [
                    domainType: candidate.sourceDomainType,
                    id        : candidate.sourceId?.toString(),
                    label     : candidate.sourceLabel
                ] as Map<String, Object>,
                target    : [
                    domainType: resultDomainType(candidate),
                    id        : resultId(candidate)?.toString(),
                    label     : resultLabel(candidate)
                ] as Map<String, Object>,
                chunk     : [
                    kind   : candidate.chunkKind,
                    ordinal: candidate.chunkOrdinal
                ] as Map<String, Object>
            ] as Map<String, Object>
        } as List<Map<String, Object>>
    }

    private static String matchText(SemanticCandidate candidate) {
        String kind = candidate.chunkKind ?: 'chunk'
        isDirectTargetMatch(candidate) ? kind : "${kind} from ${candidate.sourceDomainType} ${candidate.sourceLabel}".toString()
    }

    private static UUID resultId(SemanticCandidate candidate) {
        candidate.targetId ?: candidate.sourceId
    }

    private static String resultDomainType(SemanticCandidate candidate) {
        candidate.targetDomainType ?: candidate.sourceDomainType
    }

    private static String resultLabel(SemanticCandidate candidate) {
        candidate.targetLabel ?: candidate.sourceLabel
    }

    private static String resultDescription(SemanticCandidate candidate) {
        candidate.targetDescription ?: candidate.description
    }

    private static String round(Double value) {
        value == null ? '' : String.format(Locale.ROOT, '%.4f', value)
    }

    private static Double roundedDouble(Double value) {
        value == null ? null : Double.valueOf(String.format(Locale.ROOT, '%.4f', value))
    }

    private static class FilteredSemanticResults {
        List<SemanticSearchResultsDTO> readable
        int unreadableCount
    }
}
