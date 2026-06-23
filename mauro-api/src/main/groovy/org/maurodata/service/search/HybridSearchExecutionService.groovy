package org.maurodata.service.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
        'exact PostgreSQL keyword syntax, quoted phrase matching, OR, or exclusion must be preserved exactly; use mauro_keyword_search',
        'the user explicitly asks for semantic/vector search only; use mauro_semantic_search',
        'reading a known resource URI or id; use mauro_get'
    ],
    examples = [
        'Find forms about diabetes => searchTerm "diabetes", domainTypes ["DataModel"]',
        'Find data models about risk assessments => searchTerm "risk assessments", domainTypes ["DataModel"]',
        'Look up questions about smoking => searchTerm "smoking", domainTypes ["DataElement"]'
    ],
    inputSchema = '{"type":"object","properties":{"searchTerm":{"type":"string","description":"Search text. The keyword leg uses PostgreSQL full-text matching; the semantic leg embeds the same text when semantic search is available."},"query":{"type":"string","description":"Alias for searchTerm."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder","ClassificationScheme","Classifier"]},"description":"Optional catalogue domain type filter."},"max":{"type":"integer","minimum":1,"maximum":20,"description":"Maximum returned results for this page. Omit for default page size."},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for paging."},"withGuidance":{"type":"boolean","description":"Optional. Omit to use true."}},"required":["searchTerm"]}'
)
class HybridSearchExecutionService {

    private final SearchExecutionService searchExecutionService
    private final SemanticSearchService semanticSearchService

    HybridSearchExecutionService(SearchExecutionService searchExecutionService,
                                 SemanticSearchService semanticSearchService) {
        this.searchExecutionService = searchExecutionService
        this.semanticSearchService = semanticSearchService
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
        ListResponse<SearchResultsDTO> keywordResponse = searchExecutionService.executeSearch(unpagedKeywordRequest(safeRequest), itemLookup)

        ListResponse<SemanticSearchResultsDTO> semanticResponse = null
        SemanticSearchAvailability semanticAvailability = semanticSearchService.availability('catalogue-items-default')
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
            ListResponse<SearchResultsDTO> fallbackResponse = searchExecutionService.executeSearch(safeRequest, itemLookup)
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

        List<SearchResultsDTO> merged = merge(keywordResponse.items ?: [], semanticResponse.items ?: [])
        int offset = Math.max(safeRequest.offset ?: 0, 0)
        int max = safeRequest.max == null ? -1 : safeRequest.max
        List<SearchResultsDTO> page = max != null && max > 0 ?
            merged.drop(offset).take(max) :
            merged
        ListResponse<SearchResultsDTO> response = new ListResponse<SearchResultsDTO>(
            count: merged.size(),
            items: page
        )
        new HybridSearchResult(
            response: response,
            keywordCount: keywordResponse.count ?: 0,
            semanticAvailable: true,
            semanticRan: true,
            semanticCount: semanticResponse.count ?: 0,
            mergedCount: merged.size(),
            fallbackReason: null
        )
    }

    private static SearchRequestDTO unpagedKeywordRequest(SearchRequestDTO source) {
        SearchRequestDTO request = copySearchRequest(source)
        request.offset = 0
        request.max = -1
        request.sort = null
        request
    }

    private static SemanticSearchRequestDTO semanticRequest(SearchRequestDTO source) {
        SemanticSearchRequestDTO request = new SemanticSearchRequestDTO()
        copySearchFields(source, request)
        request.query = source.searchTerm
        request.searchTerm = source.searchTerm
        request.offset = 0
        request.max = Math.max(100, Math.min(Math.max((source.offset ?: 0) + (source.max ?: 10), 10), 100))
        request.topM = request.max
        request.topN = 100
        request.includeChunks = true
        request.rebuildIfEmpty = false
        request
    }

    private static SearchRequestDTO copySearchRequest(SearchRequestDTO source) {
        SearchRequestDTO request = new SearchRequestDTO()
        copySearchFields(source, request)
        request
    }

    private static void copySearchFields(SearchRequestDTO source, SearchRequestDTO target) {
        target.searchTerm = source.searchTerm
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

    private static List<SearchResultsDTO> merge(List<SearchResultsDTO> keywordItems, List<SemanticSearchResultsDTO> semanticItems) {
        Map<String, HybridResult> byKey = new LinkedHashMap<String, HybridResult>()
        for (int i = 0; i < keywordItems.size(); i++) {
            SearchResultsDTO item = keywordItems.get(i)
            HybridResult result = byKey.computeIfAbsent(key(item.domainType, item.id)) {
                new HybridResult(item: item)
            }
            result.item = mergeItem(result.item, item)
            result.keywordRank = i + 1
            result.keywordScore = keywordScore(item, i)
        }
        for (int i = 0; i < semanticItems.size(); i++) {
            SemanticSearchResultsDTO item = semanticItems.get(i)
            HybridResult result = byKey.computeIfAbsent(key(item.domainType, item.id)) {
                new HybridResult(item: item)
            }
            result.item = mergeItem(result.item, item)
            result.semanticRank = i + 1
            result.semanticScore = item.semanticScore ?: item.rerankScore ?: 0D
        }

        byKey.values().toList().sort {HybridResult left, HybridResult right ->
            (right.hybridScore() <=> left.hybridScore()) ?: (label(left) <=> label(right))
        }.collect {HybridResult result ->
            result.item
        } as List<SearchResultsDTO>
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

    private static double keywordScore(SearchResultsDTO item, int rankIndex) {
        double rankScore = 1D / (rankIndex + 1D)
        double tsRank = item.tsRank == null ? 0D : item.tsRank.doubleValue()
        Math.max(rankScore, Math.min(tsRank, 1D))
    }

    private static String key(String domainType, UUID id) {
        "${domainType}:${id}".toString()
    }

    private static String label(HybridResult result) {
        result.item?.label ?: ''
    }

    private static class HybridResult {
        SearchResultsDTO item
        Integer keywordRank
        Integer semanticRank
        Double keywordScore = 0D
        Double semanticScore = 0D

        double hybridScore() {
            double score = (keywordScore ?: 0D) * 0.60D + (semanticScore ?: 0D) * 0.40D
            if (keywordRank != null && semanticRank != null) {
                score += 0.25D
            }
            score
        }
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
