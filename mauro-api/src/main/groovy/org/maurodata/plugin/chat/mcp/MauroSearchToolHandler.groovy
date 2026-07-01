package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.service.search.AdministeredItemLookupService
import org.maurodata.service.search.HybridSearchExecutionService
import org.maurodata.service.search.HybridSearchExecutionService.HybridSearchResult
import org.maurodata.web.ListResponse

@CompileStatic
@Singleton
class MauroSearchToolHandler extends AbstractAnnotatedToolHandler {

    private static final int DEFAULT_PAGE_SIZE = 5
    private static final int DEFAULT_MAX_PAGE_SIZE = 20

    private final HybridSearchExecutionService hybridSearchExecutionService
    private final AdministeredItemLookupService administeredItemLookupService

    MauroSearchToolHandler(HybridSearchExecutionService hybridSearchExecutionService,
                           AdministeredItemLookupService administeredItemLookupService) {
        super(HybridSearchExecutionService)
        this.hybridSearchExecutionService = hybridSearchExecutionService
        this.administeredItemLookupService = administeredItemLookupService
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String searchTerm = firstNonBlank(
            asString(arguments.get('searchTerm')),
            asString(arguments.get('query')),
            asString(arguments.get('term')),
            asString(arguments.get('text'))
        )
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'mauro_search requires searchTerm or query')
        }

        SearchRequestDTO request = new SearchRequestDTO()
        request.searchTerm = searchTerm
        request.domainTypes = extractStringList(arguments.get('domainTypes'))
        request.withinModelId = asUuid(arguments.get('modelId') ?: arguments.get('withinModelId'))
        Integer requestedMax = asInteger(arguments.get('max'), DEFAULT_PAGE_SIZE)
        request.max = Math.min(Math.max(requestedMax, 1), DEFAULT_MAX_PAGE_SIZE)
        request.offset = asInteger(arguments.get('offset'), 0)
        boolean withGuidance = asBoolean(arguments.get('withGuidance'), true)

        HybridSearchResult hybridResult = hybridSearchExecutionService.executeSearchDetailed(
            request,
            { String domainType, UUID id -> administeredItemLookupService.findAdministeredItem(domainType, id) }
        )
        ListResponse<SearchResultsDTO> response = hybridResult.response

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>()
        for (SearchResultsDTO result : response.items ?: []) {
            Map<String, Object> row = [
                id: result.id?.toString(),
                domainType: result.domainType,
                label: result.label,
                description: result.description,
                tsRank: roundScore(result.tsRank)
            ] as Map<String, Object>
            if (result instanceof SemanticSearchResultsDTO) {
                SemanticSearchResultsDTO semantic = (SemanticSearchResultsDTO) result
                row.put('semanticScore', roundScore(semantic.semanticScore))
                row.put('rerankScore', roundScore(semantic.rerankScore))
                row.put('hybridScore', roundScore(semantic.hybridScore))
                row.put('keywordRank', semantic.keywordRank)
                row.put('semanticRank', semantic.semanticRank)
                row.put('matchedChunkCount', semantic.matchedChunkCount)
                row.put('embeddingProfiles', semantic.embeddingProfiles)
                row.put('evidence', semantic.evidence)
                row.put('evidenceDetails', semantic.evidenceDetails)
            }
            items.add(row)
        }

        [
            searchTerm: searchTerm,
            domainTypes: request.domainTypes,
            modelId: request.withinModelId?.toString(),
            count: response.count ?: 0,
            max: request.max,
            offset: request.offset,
            nextOffset: request.offset + items.size(),
            hasMore: (request.offset + items.size()) < (response.count ?: 0),
            withGuidance: withGuidance,
            keywordCount: hybridResult.keywordCount,
            semanticAvailable: hybridResult.semanticAvailable,
            semanticRan: hybridResult.semanticRan,
            semanticCount: hybridResult.semanticCount,
            mergedCount: hybridResult.mergedCount,
            fallbackReason: hybridResult.fallbackReason,
            items: items
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> items = result.get('items') instanceof List ? (List<?>) result.get('items') : Collections.emptyList()
        int totalCount = asInteger(result.get('count'), items.size())
        int returnedCount = items.size()
        int offset = asInteger(result.get('offset'), 0)
        int max = Math.max(asInteger(result.get('max'), returnedCount == 0 ? DEFAULT_PAGE_SIZE : returnedCount), 1)
        int nextOffset = asInteger(result.get('nextOffset'), offset + returnedCount)
        boolean hasMore = Boolean.TRUE.equals(result.get('hasMore'))
        String searchTerm = asString(result.get('searchTerm')) ?: ''
        List<String> domainTypes = extractStringList(result.get('domainTypes'))
        String modelId = asString(result.get('modelId'))
        boolean semanticEvidencePresent = items.any {Object item ->
            item instanceof Map && ((Map<?, ?>) item).containsKey('semanticScore')
        }
        boolean semanticRan = Boolean.TRUE.equals(result.get('semanticRan'))
        boolean semanticAvailable = Boolean.TRUE.equals(result.get('semanticAvailable'))
        int pageNumber = offset.intdiv(max) + 1
        int totalPages = Math.max((int) Math.ceil(totalCount / (double) max), returnedCount > 0 ? 1 : 0)
        int rangeStart = returnedCount > 0 ? offset + 1 : 0
        int rangeEnd = offset + returnedCount

        List<String> returnedData = new ArrayList<String>()
        List<String> retrievalEvidence = new ArrayList<String>()
        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i)
            if (!(itemObj instanceof Map)) {
                continue
            }
            Map<?, ?> item = (Map<?, ?>) itemObj
            returnedData.add("${i + 1}. Label: ${item.get('label')}; Type: ${item.get('domainType')}; ID: ${item.get('id')}; Description: ${item.get('description') ?: ''}".toString())
            if (semanticEvidencePresent) {
                retrievalEvidence.add("${i + 1}. Keyword rank: ${item.get('keywordRank') ?: item.get('tsRank')}; Semantic rank: ${item.get('semanticRank') ?: ''}; Hybrid score: ${item.get('hybridScore') ?: ''}; Semantic score: ${item.get('semanticScore') ?: ''}; Evidence: ${evidenceText(item.get('evidenceDetails') ?: item.get('evidence'))}".toString())
            }
        }

        List<String> instructions = [
            semanticRan ? 'COMMON: Say that you searched the Mauro catalogue for matching and related items.' : 'COMMON: Say that you searched the Mauro catalogue for matching items.',
            'COMMON: Do not mention internal retrieval evidence, semantic availability, hybrid scores, keyword ranks, semantic ranks, or embedding profiles unless the user asks how the search was ranked.',
            "COMMON: Tell the user the exact search term used: ${searchTerm}".toString(),
            "COMMON: Tell the user the total number of matching catalogue items is ${totalCount}.".toString(),
            "COMMON: Use this pagination summary in your answer: Page ${pageNumber} of ${totalPages}. Showing ${rangeStart}-${rangeEnd} of ${totalCount} matching catalogue items, ${max} results at a time.".toString(),
            'COMMON: Present the returned matches as a Markdown table. Use columns Label, Type, ID, Description.',
            'COMMON: Escape pipe characters inside Markdown table cell values as \\|.',
            'COMMON: When the user refers to an ordinal result, use the ID from the matching numbered Returned Data item.'
        ] as List<String>
        if (!domainTypes.isEmpty()) {
            instructions.add("COMMON: Tell the user these results are filtered to ${domainTypes.join(', ')} only.".toString())
        }

        List<String> followUp = new ArrayList<String>()
        if (hasMore) {
            Map<String, Object> nextPageToolCall = [
                name: 'mauro_search',
                arguments: [
                    searchTerm: searchTerm,
                    domainTypes: domainTypes,
                    modelId: modelId,
                    max: max,
                    offset: nextOffset,
                    withGuidance: result.get('withGuidance') != false
                ] as Map<String, Object>
            ] as Map<String, Object>
            if (!modelId) {
                ((Map<String, Object>) nextPageToolCall.arguments).remove('modelId')
            }
            followUp.add('FR: More results are available.')
            followUp.add('FR: In a later turn, if the user asks for the next page, use this exact tool call: ' + JsonOutput.toJson(nextPageToolCall))
        }

        renderModelTextSections([
            'Tool Call Status': ['Tool mauro_search succeeded.'],
            'Result Metadata': [
                "Search term: ${searchTerm}",
                "Total matching catalogue items: ${totalCount}",
                "Keyword leg count: ${result.get('keywordCount')}",
                "Semantic available: ${semanticAvailable}",
                "Semantic leg ran: ${semanticRan}",
                "Semantic leg count: ${result.get('semanticCount')}",
                "Merged result count: ${result.get('mergedCount')}",
                result.get('fallbackReason') ? "Semantic fallback reason: ${result.get('fallbackReason')}" : null,
                "Returned items for this page: ${returnedCount}",
                "Page: ${pageNumber} of ${totalPages}",
                "Visible range: ${rangeStart}-${rangeEnd} of ${totalCount}",
                "Domain type filter: ${domainTypes.join(', ')}",
                modelId ? "Model scope: ${modelId}" : null,
                "Semantic evidence present: ${semanticEvidencePresent}",
                "Has more results: ${hasMore}"
            ].findAll {Object value -> value != null},
            'Returned Data': returnedData ?: ['No results were returned.'],
            'Retrieval Evidence For Assistant': retrievalEvidence,
            'Answer Instructions': instructions,
            'Follow-up Actions': followUp
        ] as Map<String, Object>)
    }

    private static List<String> extractStringList(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).collect {Object item -> String.valueOf(item)}.findAll {String item -> item?.trim()} as List<String>
        }
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return []
        }
        String.valueOf(value).split(/\s*,\s*/).findAll {String item -> item?.trim()} as List<String>
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim()
            }
        }
        null
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static UUID asUuid(Object value) {
        String text = asString(value)
        text == null || text.trim().isEmpty() ? null : UUID.fromString(text.trim())
    }

    private static int asInteger(Object value, int fallback) {
        if (value == null) {
            return fallback
        }
        value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(String.valueOf(value))
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }

    private static String evidenceText(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).take(3).collect {Object item -> evidenceItemText(item)}.join(' | ')
        }
        value == null ? '' : String.valueOf(value)
    }

    private static String evidenceItemText(Object item) {
        if (!(item instanceof Map)) {
            return String.valueOf(item)
        }
        Map<?, ?> evidence = (Map<?, ?>) item
        String match = asString(evidence.get('match')) ?: ''
        Object confidence = evidence.get('confidence')
        String confidenceText = confidenceText(confidence)
        confidenceText ? "${match} (${confidenceText})".toString() : match
    }

    private static String confidenceText(Object confidence) {
        if (confidence instanceof Map) {
            return ((Map<?, ?>) confidence).collect {Map.Entry<?, ?> entry ->
                "${entry.key}=${entry.value}".toString()
            }.join(', ')
        }
        confidence == null ? '' : String.valueOf(confidence)
    }

    private static Double roundScore(Object value) {
        if (value == null) {
            return null
        }
        Double number = value instanceof Number ? ((Number) value).doubleValue() : Double.valueOf(String.valueOf(value))
        Double.valueOf(String.format(Locale.ROOT, '%.4f', number))
    }
}
