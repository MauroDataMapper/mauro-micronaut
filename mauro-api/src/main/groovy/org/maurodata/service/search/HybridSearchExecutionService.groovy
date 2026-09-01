package org.maurodata.service.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
class HybridSearchExecutionService {

    private final SearchExecutionService searchExecutionService
    private final SemanticSearchService semanticSearchService
    private final double keywordWeight
    private final double semanticWeight
    private final int rankConstant
    private final int rankWindow
    private final int keywordFetchMultiplier
    private final int resultCacheSize
    private final long resultCacheTtlMillis
    private final Map<String, CachedHybridCandidates> resultCache

    HybridSearchExecutionService(SearchExecutionService searchExecutionService,
                                 SemanticSearchService semanticSearchService,
                                 @Value('${mauro.search.semantic.hybrid.keyword-weight:1.0}') Double keywordWeight,
                                 @Value('${mauro.search.semantic.hybrid.semantic-weight:1.0}') Double semanticWeight,
                                 @Value('${mauro.search.semantic.hybrid.rank-constant:60}') Integer rankConstant,
                                 @Value('${mauro.search.semantic.hybrid.rank-window:100}') Integer rankWindow,
                                 @Value('${mauro.search.semantic.hybrid.keyword-fetch-multiplier:10}') Integer keywordFetchMultiplier,
                                 @Value('${mauro.search.semantic.hybrid.result-cache-size:128}') Integer resultCacheSize,
                                 @Value('${mauro.search.semantic.hybrid.result-cache-ttl-ms:300000}') Long resultCacheTtlMillis) {
        this.searchExecutionService = searchExecutionService
        this.semanticSearchService = semanticSearchService
        this.keywordWeight = keywordWeight == null ? 1.0D : Math.max(keywordWeight, 0D)
        this.semanticWeight = semanticWeight == null ? 1.0D : Math.max(semanticWeight, 0D)
        this.rankConstant = Math.max(rankConstant ?: 60, 1)
        this.rankWindow = Math.max(rankWindow ?: 100, 1)
        this.keywordFetchMultiplier = Math.max(keywordFetchMultiplier ?: 10, 1)
        this.resultCacheSize = Math.max(resultCacheSize ?: 128, 0)
        this.resultCacheTtlMillis = Math.max(resultCacheTtlMillis ?: 300000L, 0L)
        this.resultCache = Collections.synchronizedMap(new LinkedHashMap<String, CachedHybridCandidates>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedHybridCandidates> eldest) {
                HybridSearchExecutionService.this.resultCacheSize > 0 &&
                    size() > HybridSearchExecutionService.this.resultCacheSize
            }
        })
    }

    @Connectable
    ListResponse<SearchResultsDTO> executeSearch(SearchRequestDTO requestDTO,
                                                 BiFunction<String, UUID, AdministeredItem> itemLookup) {
        executeSearchDetailed(requestDTO, itemLookup).response
    }

    @Connectable
    HybridSearchResult executeSearchDetailed(SearchRequestDTO requestDTO,
                                             BiFunction<String, UUID, AdministeredItem> itemLookup) {
        long totalStart = System.currentTimeMillis()
        SearchRequestDTO safeRequest = requestDTO ?: new SearchRequestDTO()
        int offset = safeRequest.offset ?: 0
        int max = safeRequest.max != null && safeRequest.max > 0 ? safeRequest.max : -1
        int requiredReadable = max > 0 ? Math.max(offset + max, max) : Integer.MAX_VALUE
        boolean projectKeywordResults = safeRequest.domainTypes != null && !safeRequest.domainTypes.isEmpty()
        SemanticSearchAvailability semanticAvailability = semanticSearchService.availability(safeRequest.corpus, safeRequest.withinModelId)
        long availabilityMillis = System.currentTimeMillis() - totalStart
        if (!semanticAvailability.available) {
            long keywordStart = System.currentTimeMillis()
            ListResponse<SearchResultsDTO> keywordOnlyResponse = searchExecutionService.executeSearch(safeRequest, itemLookup)
            long keywordMillis = System.currentTimeMillis() - keywordStart
            log.info(
                'Hybrid search timing query="{}" semanticRan=false fallbackToKeyword=true keyword={} returned={} countIsExact={} timingsMs={availability={}, keyword={}, total={}}',
                safeRequest.searchTerm,
                keywordOnlyResponse.count ?: 0,
                keywordOnlyResponse.items?.size() ?: 0,
                keywordOnlyResponse.countIsExact,
                availabilityMillis,
                keywordMillis,
                System.currentTimeMillis() - totalStart
            )
            return new HybridSearchResult(
                response: keywordOnlyResponse,
                keywordCount: keywordOnlyResponse.count ?: 0,
                semanticAvailable: false,
                semanticRan: false,
                semanticCount: 0,
                mergedCount: keywordOnlyResponse.count ?: 0,
                countIsExact: keywordOnlyResponse.countIsExact,
                fallbackReason: semanticAvailability.reason
            )
        }

        int keywordLimit = keywordFetchLimit(safeRequest, projectKeywordResults, requiredReadable)
        String cacheKey = cacheKey(safeRequest, projectKeywordResults, keywordLimit)
        CachedHybridCandidates cached = readCache(cacheKey)
        boolean cacheHit = cached != null
        long keywordMillis = 0L
        long semanticMillis = 0L
        long mergeMillis = 0L
        CachedHybridCandidates candidates = cached
        String fallbackReason = null
        if (candidates == null) {
            long keywordStart = System.currentTimeMillis()
            List<SearchResultsDTO> rawKeywordItems = searchExecutionService.retrieveSearchResults(
                unpagedKeywordRequest(safeRequest, projectKeywordResults),
                keywordLimit
            )
            List<SearchResultsDTO> keywordItems = projectKeywordResults ?
                semanticSearchService.projectResults(rawKeywordItems, safeRequest.domainTypes) :
                rawKeywordItems
            List<SearchResultsDTO> keywordMergeItems = keywordItems.take(rankWindow)
            keywordMillis = System.currentTimeMillis() - keywordStart
            int keywordCount = projectKeywordResults ?
                keywordItems.collect {SearchResultsDTO item -> key(item.domainType, item.id)}.unique().size() :
                keywordItems.size()

            ListResponse<SemanticSearchResultsDTO> semanticResponse = null
            try {
                long semanticStart = System.currentTimeMillis()
                semanticResponse = semanticSearchService.executeSearch(semanticRequest(safeRequest), itemLookup)
                semanticMillis = System.currentTimeMillis() - semanticStart
            } catch (Exception e) {
                log.warn('Hybrid search semantic leg failed; falling back to keyword-only search', e)
                fallbackReason = e.message ?: e.class.simpleName
            }

            if (semanticResponse == null) {
                long fallbackKeywordStart = System.currentTimeMillis()
                ListResponse<SearchResultsDTO> fallbackResponse = searchExecutionService.executeSearch(safeRequest, itemLookup)
                long fallbackKeywordMillis = System.currentTimeMillis() - fallbackKeywordStart
                log.info(
                    'Hybrid search timing query="{}" semanticRan=false fallbackToKeyword=true cacheHit=false keyword={} keywordLimit={} projected={} returned={} countIsExact={} timingsMs={availability={}, keyword={}, semantic={}, fallbackKeyword={}, total={}}',
                    safeRequest.searchTerm,
                    rawKeywordItems.size(),
                    keywordLimit,
                    keywordItems.size(),
                    fallbackResponse.items?.size() ?: 0,
                    fallbackResponse.countIsExact,
                    availabilityMillis,
                    keywordMillis,
                    semanticMillis,
                    fallbackKeywordMillis,
                    System.currentTimeMillis() - totalStart
                )
                return new HybridSearchResult(
                    response: fallbackResponse,
                    keywordCount: keywordCount,
                    semanticAvailable: false,
                    semanticRan: false,
                    semanticCount: 0,
                    mergedCount: fallbackResponse.count ?: 0,
                    countIsExact: fallbackResponse.countIsExact,
                    fallbackReason: fallbackReason ?: 'semantic search did not return a result'
                )
            }

            long mergeStart = System.currentTimeMillis()
            MergeResult mergeResult = merge(keywordMergeItems, keywordCount, semanticResponse.items ?: [], semanticResponse.count ?: 0)
            mergeMillis = System.currentTimeMillis() - mergeStart
            candidates = new CachedHybridCandidates(
                items: mergeResult.items,
                keywordCount: keywordCount,
                rawKeywordCount: rawKeywordItems.size(),
                projectedKeywordCount: keywordItems.size(),
                semanticCount: semanticResponse.count ?: 0,
                mergedCandidateCount: mergeResult.items.size(),
                createdAtMillis: System.currentTimeMillis()
            )
            writeCache(cacheKey, candidates)
        }

        long accessStart = System.currentTimeMillis()
        SearchExecutionService.FilteredSearchResults filtered = searchExecutionService.filterReadableUntil(candidates.items, safeRequest, itemLookup, requiredReadable)
        long accessMillis = System.currentTimeMillis() - accessStart
        List<SearchResultsDTO> page = max > 0 ? filtered.readable.drop(offset).take(max) : filtered.readable
        int responseCount = filtered.exhausted ?
            filtered.readable.size() :
            Math.max(filtered.readable.size(), offset + page.size() + 1)
        ListResponse<SearchResultsDTO> response = new ListResponse<SearchResultsDTO>(
            count: responseCount,
            countIsExact: filtered.exhausted,
            items: page
        )
        log.info(
            'Hybrid search timing query="{}" semanticRan=true cacheHit={} keyword={} keywordLimit={} projected={} semantic={} merged={} accessScanned={} checkedAllForAccess={} returned={} countIsExact={} timingsMs={keyword={}, availability={}, semantic={}, merge={}, accessFilter={}, total={}}',
            safeRequest.searchTerm,
            cacheHit,
            candidates.rawKeywordCount,
            keywordLimit,
            candidates.projectedKeywordCount,
            candidates.semanticCount,
            candidates.mergedCandidateCount,
            filtered.scannedCount,
            filtered.exhausted,
            page.size(),
            filtered.exhausted,
            keywordMillis,
            availabilityMillis,
            semanticMillis,
            mergeMillis,
            accessMillis,
            System.currentTimeMillis() - totalStart
        )
        new HybridSearchResult(
            response: response,
            keywordCount: candidates.keywordCount,
            semanticAvailable: true,
            semanticRan: true,
            semanticCount: candidates.semanticCount,
            mergedCount: response.count ?: 0,
            countIsExact: response.countIsExact,
            fallbackReason: null
        )
    }

    private static SearchRequestDTO unpagedKeywordRequest(SearchRequestDTO source, boolean clearDomainTypes = false) {
        SearchRequestDTO request = copySearchRequest(source)
        request.offset = 0
        request.max = -1
        request.sort = null
        if (clearDomainTypes) {
            request.domainTypes = Collections.<String>emptyList()
            request.domainType = null
        }
        request
    }

    private SemanticSearchRequestDTO semanticRequest(SearchRequestDTO source) {
        SemanticSearchRequestDTO request = new SemanticSearchRequestDTO()
        copySearchFields(source, request)
        request.query = source.searchTerm
        request.searchTerm = source.searchTerm
        request.offset = 0
        request.max = Math.max(rankWindow, Math.min(Math.max((source.offset ?: 0) + (source.max ?: 10), 10), rankWindow))
        request.topM = request.max
        request.topN = rankWindow
        request.includeChunks = true
        request.rebuildIfEmpty = false
        request.deepSearch = source.deepSearch == true
        request
    }

    private int keywordFetchLimit(SearchRequestDTO request, boolean projectKeywordResults, int requiredReadable) {
        if (request.deepSearch == true || requiredReadable == Integer.MAX_VALUE) {
            return -1
        }
        int base = Math.max(rankWindow, requiredReadable)
        long calculated = projectKeywordResults ? (long) base * keywordFetchMultiplier : (long) base
        calculated > Integer.MAX_VALUE ? Integer.MAX_VALUE : calculated.intValue()
    }

    private CachedHybridCandidates readCache(String key) {
        if (resultCacheSize <= 0 || resultCacheTtlMillis <= 0L || key == null) {
            return null
        }
        CachedHybridCandidates cached = resultCache.get(key)
        if (cached == null) {
            return null
        }
        if (System.currentTimeMillis() - cached.createdAtMillis > resultCacheTtlMillis) {
            resultCache.remove(key)
            return null
        }
        cached
    }

    private void writeCache(String key, CachedHybridCandidates value) {
        if (resultCacheSize <= 0 || resultCacheTtlMillis <= 0L || key == null || value == null) {
            return
        }
        resultCache.put(key, value)
    }

    private String cacheKey(SearchRequestDTO request, boolean projectKeywordResults, int keywordLimit) {
        [
            'hybrid-v1',
            rankWindow,
            rankConstant,
            keywordWeight,
            semanticWeight,
            keywordFetchMultiplier,
            keywordLimit,
            projectKeywordResults,
            normalizedString(request.searchTerm),
            normalizedString(request.corpus),
            request.withinModelId?.toString() ?: '',
            request.prefixSearch == true,
            request.deepSearch == true,
            normalizedStrings(request.domainTypes),
            normalizedStrings(request.classifiers == null ? Collections.<String>emptyList() : request.classifiers.collect {UUID id -> id.toString()} as List<String>),
            dateKey(request.createdBefore),
            dateKey(request.createdAfter),
            dateKey(request.lastUpdatedBefore),
            dateKey(request.lastUpdatedAfter),
            normalizedString(request.domainType)
        ].join('|')
    }

    private static String normalizedString(String value) {
        value == null ? '' : value.trim()
    }

    private static String normalizedStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return ''
        }
        new ArrayList<String>(values.findAll {String value -> value != null}.collect {String value -> value.trim()} as List<String>)
            .sort()
            .join(',')
    }

    private static String dateKey(Object value) {
        value == null ? '' : String.valueOf(value)
    }

    private static SearchRequestDTO copySearchRequest(SearchRequestDTO source) {
        SearchRequestDTO request = new SearchRequestDTO()
        copySearchFields(source, request)
        request
    }

    private static void copySearchFields(SearchRequestDTO source, SearchRequestDTO target) {
        target.searchTerm = source.searchTerm
        target.corpus = source.corpus
        target.domainTypes = source.domainTypes == null ? Collections.<String>emptyList() : new ArrayList<String>(source.domainTypes)
        target.withinModelId = source.withinModelId
        target.prefixSearch = source.prefixSearch
        target.deepSearch = source.deepSearch
        target.lastUpdatedAfter = source.lastUpdatedAfter
        target.lastUpdatedBefore = source.lastUpdatedBefore
        target.createdAfter = source.createdAfter
        target.createdBefore = source.createdBefore
        target.classifiers = source.classifiers == null ? null : new ArrayList<UUID>(source.classifiers)
        target.offset = source.offset
        target.max = source.max
        target.sort = source.sort
        target.order = source.order
        target.label = source.label
        target.description = source.description
        target.code = source.code
        target.definition = source.definition
        target.all = source.all
        target.domainType = source.domainType
    }

    private MergeResult merge(List<SearchResultsDTO> keywordItems,
                              int keywordCount,
                              List<SemanticSearchResultsDTO> semanticItems,
                              int semanticCount) {
        Map<String, HybridResult> byKey = new LinkedHashMap<String, HybridResult>()
        for (int i = 0; i < Math.min(keywordItems.size(), rankWindow); i++) {
            SearchResultsDTO item = keywordItems.get(i)
            HybridResult result = byKey.computeIfAbsent(key(item.domainType, item.id)) {
                new HybridResult(item: item)
            }
            result.item = mergeItem(result.item, item)
            result.keywordRank = i + 1
            result.keywordScore = reciprocalRank(i + 1, keywordWeight)
        }
        for (int i = 0; i < Math.min(semanticItems.size(), rankWindow); i++) {
            SemanticSearchResultsDTO item = semanticItems.get(i)
            HybridResult result = byKey.computeIfAbsent(key(item.domainType, item.id)) {
                new HybridResult(item: item)
            }
            result.item = mergeItem(result.item, item)
            result.semanticRank = i + 1
            result.semanticScore = reciprocalRank(i + 1, semanticWeight)
        }

        List<SearchResultsDTO> items = byKey.values().toList().sort {HybridResult left, HybridResult right ->
            (left.rankBucket() <=> right.rankBucket()) ?:
                (right.hybridScore() <=> left.hybridScore()) ?:
                (stableKey(left) <=> stableKey(right))
        }.collect {HybridResult result ->
            applyHybridEvidence(result)
            result.item
        } as List<SearchResultsDTO>
        int semanticOnlyInWindow = byKey.values().count {HybridResult result ->
            result.semanticRank != null && result.keywordRank == null
        }.intValue()
        new MergeResult(
            items: items,
            count: Math.max(items.size(), keywordCount + Math.min(semanticOnlyInWindow, Math.max(semanticCount, 0)))
        )
    }

    private static void applyHybridEvidence(HybridResult result) {
        if (!(result.item instanceof SemanticSearchResultsDTO)) {
            return
        }
        SemanticSearchResultsDTO semantic = (SemanticSearchResultsDTO) result.item
        semantic.keywordRank = result.keywordRank
        semantic.semanticRank = result.semanticRank
        semantic.hybridScore = result.hybridScore()
        List<String> evidence = semantic.evidence == null ? new ArrayList<String>() : new ArrayList<String>(semantic.evidence)
        if (result.keywordRank != null) {
            evidence.add(0, "keyword rank ${result.keywordRank}".toString())
        }
        if (result.semanticRank != null) {
            evidence.add(result.keywordRank == null ? 0 : 1, "semantic rank ${result.semanticRank}".toString())
        }
        semantic.evidence = evidence.unique() as List<String>

        List<Map<String, Object>> evidenceDetails = semantic.evidenceDetails == null ?
            new ArrayList<Map<String, Object>>() :
            new ArrayList<Map<String, Object>>(semantic.evidenceDetails)
        if (result.semanticRank != null) {
            evidenceDetails.add(0, [match: 'semantic', confidence: "rank ${result.semanticRank}".toString()] as Map<String, Object>)
        }
        if (result.keywordRank != null) {
            evidenceDetails.add(0, [match: 'keyword', confidence: "rank ${result.keywordRank}".toString()] as Map<String, Object>)
        }
        semantic.evidenceDetails = evidenceDetails
    }

    private static SearchResultsDTO mergeItem(SearchResultsDTO existing, SearchResultsDTO incoming) {
        if (existing == null) {
            return incoming
        }
        if (existing instanceof SemanticSearchResultsDTO) {
            return mergeIntoSemantic((SemanticSearchResultsDTO) existing, incoming)
        }
        if (incoming instanceof SemanticSearchResultsDTO) {
            return mergeIntoSemantic((SemanticSearchResultsDTO) incoming, existing)
        }
        existing.tsRank = existing.tsRank ?: incoming.tsRank
        existing
    }

    private static SemanticSearchResultsDTO mergeIntoSemantic(SemanticSearchResultsDTO semantic, SearchResultsDTO other) {
        semantic.tsRank = semantic.tsRank ?: other.tsRank
        semantic.breadcrumbs = semantic.breadcrumbs ?: other.breadcrumbs
        if ((semantic.classifiers == null || semantic.classifiers.isEmpty()) && other.classifiers != null) {
            semantic.classifiers = other.classifiers
        }
        semantic
    }

    private double reciprocalRank(int rank, double weight) {
        weight / (rankConstant + rank)
    }

    private static String key(String domainType, UUID id) {
        "${domainType}:${id}".toString()
    }

    private static String label(HybridResult result) {
        result.item?.label ?: ''
    }

    private static String stableKey(HybridResult result) {
        [
            result.item?.domainType ?: '',
            result.item?.label ?: '',
            result.item?.id?.toString() ?: ''
        ].join('|')
    }

    private static class HybridResult {
        SearchResultsDTO item
        Integer keywordRank
        Integer semanticRank
        Double keywordScore = 0D
        Double semanticScore = 0D

        double hybridScore() {
            double score = (keywordScore ?: 0D) + (semanticScore ?: 0D)
            if (keywordRank != null && semanticRank != null) {
                score += 0.005D
            }
            score
        }

        int rankBucket() {
            if (keywordRank != null && semanticRank != null) {
                return 0
            }
            if (keywordRank != null) {
                return 1
            }
            2
        }
    }

    private static class MergeResult {
        List<SearchResultsDTO> items = []
        Integer count = 0
    }

    private static class CachedHybridCandidates {
        List<SearchResultsDTO> items = []
        Integer keywordCount = 0
        Integer rawKeywordCount = 0
        Integer projectedKeywordCount = 0
        Integer semanticCount = 0
        Integer mergedCandidateCount = 0
        Long createdAtMillis = 0L
    }

    static class HybridSearchResult {
        ListResponse<SearchResultsDTO> response
        Integer keywordCount = 0
        Boolean semanticAvailable = false
        Boolean semanticRan = false
        Integer semanticCount = 0
        Integer mergedCount = 0
        Boolean countIsExact = true
        String fallbackReason
    }

}
