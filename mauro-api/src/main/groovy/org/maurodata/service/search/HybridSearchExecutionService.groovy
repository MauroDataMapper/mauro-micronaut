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
import org.maurodata.service.chat.mcp.McpToolDefinition
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_search',
    description = 'Search Mauro catalogue content using combined keyword and semantic retrieval.',
    purpose = 'Run a hybrid Mauro catalogue search that combines PostgreSQL keyword/full-text search with semantic/vector search when a usable semantic index is available. Falls back to keyword-only search when semantic search is not available.',
    useWhen = [
        'finding, searching, listing, or inspecting live catalogue content in the connected Mauro instance',
        'finding forms, Data Models, Data Classes, Data Elements, Terms, metadata, or other catalogue items about a subject',
        'searching broadly where exact labels and semantically related wording may both be useful'
    ],
    avoidWhen = [
        'the user explicitly asks for a narrower specialist search mode only',
        'reading a known resource URI or id; use mauro_get'
    ],
    examples = [
        'Find forms about diabetes => searchTerm "diabetes", domainTypes ["DataModel"]',
        'Find data models about risk assessments => searchTerm "risk assessments", domainTypes ["DataModel"]',
        'Look up questions about smoking => searchTerm "smoking", domainTypes ["DataElement"]'
    ],
    inputSchema = '{"type":"object","properties":{"searchTerm":{"type":"string","description":"Search text. The keyword leg uses PostgreSQL full-text matching; the semantic leg embeds the same text when semantic search is available."},"query":{"type":"string","description":"Alias for searchTerm."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder","ClassificationScheme","Classifier"]},"description":"Optional catalogue domain type filter."},"modelId":{"type":"string","format":"uuid","description":"Optional UUID of a DataModel, Terminology, CodeSet, Folder, or VersionedFolder to scope the search. Folder scopes include descendant folders and contained models."},"corpus":{"type":"string","description":"Optional API-visible semantic corpus name used to constrain the semantic leg. Omit to search API-visible semantic corpora for the requested model scope."},"max":{"type":"integer","minimum":1,"maximum":20,"description":"Maximum returned results for this page. Omit for default page size."},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for paging."},"withGuidance":{"type":"boolean","description":"Optional. Omit to use true."}},"required":["searchTerm"]}'
)
class HybridSearchExecutionService {

    private final SearchExecutionService searchExecutionService
    private final SemanticSearchService semanticSearchService
    private final double keywordWeight
    private final double semanticWeight
    private final int rankConstant
    private final int rankWindow

    HybridSearchExecutionService(SearchExecutionService searchExecutionService,
                                 SemanticSearchService semanticSearchService,
                                 @Value('${chat.semantic.hybrid.keyword-weight:1.0}') Double keywordWeight,
                                 @Value('${chat.semantic.hybrid.semantic-weight:1.0}') Double semanticWeight,
                                 @Value('${chat.semantic.hybrid.rank-constant:60}') Integer rankConstant,
                                 @Value('${chat.semantic.hybrid.rank-window:100}') Integer rankWindow) {
        this.searchExecutionService = searchExecutionService
        this.semanticSearchService = semanticSearchService
        this.keywordWeight = keywordWeight == null ? 1.0D : Math.max(keywordWeight, 0D)
        this.semanticWeight = semanticWeight == null ? 1.0D : Math.max(semanticWeight, 0D)
        this.rankConstant = Math.max(rankConstant ?: 60, 1)
        this.rankWindow = Math.max(rankWindow ?: 100, 1)
    }

    @Connectable
    ListResponse<SearchResultsDTO> executeSearch(SearchRequestDTO requestDTO,
                                                 BiFunction<String, UUID, AdministeredItem> itemLookup) {
        executeSearchDetailed(requestDTO, itemLookup).response
    }

    @Connectable
    HybridSearchResult executeSearchDetailed(SearchRequestDTO requestDTO,
                                             BiFunction<String, UUID, AdministeredItem> itemLookup) {
        SearchRequestDTO safeRequest = requestDTO ?: new SearchRequestDTO()
        boolean projectKeywordResults = safeRequest.domainTypes != null && !safeRequest.domainTypes.isEmpty()
        List<SearchResultsDTO> rawKeywordItems = searchExecutionService.retrieveSearchResults(unpagedKeywordRequest(safeRequest, projectKeywordResults))
        List<SearchResultsDTO> keywordItems = projectKeywordResults ?
            semanticSearchService.projectResults(rawKeywordItems, safeRequest.domainTypes) :
            rawKeywordItems
        int keywordCount = projectKeywordResults ?
            keywordItems.collect {SearchResultsDTO item -> key(item.domainType, item.id)}.unique().size() :
            keywordItems.size()
        ListResponse<SearchResultsDTO> keywordResponse = new ListResponse<SearchResultsDTO>(
            count: keywordCount,
            items: keywordItems
        )

        ListResponse<SemanticSearchResultsDTO> semanticResponse = null
        SemanticSearchAvailability semanticAvailability = semanticSearchService.availability(safeRequest.corpus, safeRequest.withinModelId)
        String fallbackReason = null
        if (semanticAvailability.available) {
            try {
                semanticResponse = semanticSearchService.executeSearch(semanticRequest(safeRequest), itemLookup)
            } catch (Exception e) {
                log.warn('Hybrid search semantic leg failed; falling back to keyword-only search', e)
                fallbackReason = e.message ?: e.class.simpleName
            }
        } else {
            fallbackReason = semanticAvailability.reason
        }

        if (semanticResponse == null) {
            List<SearchResultsDTO> readableKeywordItems = searchExecutionService.filterReadable(keywordResponse.items ?: [], safeRequest, itemLookup)
            ListResponse<SearchResultsDTO> fallbackResponse = ListResponse.from(readableKeywordItems, safeRequest)
            return new HybridSearchResult(
                response: fallbackResponse,
                keywordCount: keywordResponse.count ?: 0,
                semanticAvailable: false,
                semanticRan: false,
                semanticCount: 0,
                mergedCount: fallbackResponse.count ?: 0,
                fallbackReason: fallbackReason ?: 'semantic search did not return a result'
            )
        }

        MergeResult mergeResult = merge(keywordResponse.items ?: [], keywordResponse.count ?: 0, semanticResponse.items ?: [], semanticResponse.count ?: 0)
        List<SearchResultsDTO> readableMerged = searchExecutionService.filterReadable(mergeResult.items, safeRequest, itemLookup)
        ListResponse<SearchResultsDTO> response = ListResponse.from(readableMerged, safeRequest)
        new HybridSearchResult(
            response: response,
            keywordCount: keywordResponse.count ?: 0,
            semanticAvailable: true,
            semanticRan: true,
            semanticCount: semanticResponse.count ?: 0,
            mergedCount: response.count ?: 0,
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
        request.deepSearch = false
        request
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

    static class HybridSearchResult {
        ListResponse<SearchResultsDTO> response
        Integer keywordCount = 0
        Boolean semanticAvailable = false
        Boolean semanticRan = false
        Integer semanticCount = 0
        Integer mergedCount = 0
        String fallbackReason
    }

}
