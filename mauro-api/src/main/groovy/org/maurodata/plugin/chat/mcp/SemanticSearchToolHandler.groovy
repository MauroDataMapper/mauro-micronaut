package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SemanticChunkMatchDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.service.search.AdministeredItemLookupService
import org.maurodata.service.search.SemanticSearchAvailability
import org.maurodata.service.search.SemanticSearchService
import org.maurodata.web.ListResponse

@CompileStatic
@Singleton
@Slf4j
@McpToolDefinition(
    name = 'mauro_semantic_search',
    description = 'Search Mauro catalogue content by semantic similarity only using embedding profiles.',
    purpose = 'Run the narrower semantic/vector-only catalogue search. For ordinary catalogue searches use mauro_search, which combines keyword search with semantic search when available. Use this specialist tool only when the user explicitly asks for semantic/vector search or embedding-similarity behaviour.',
    useWhen = [
        'the user explicitly asks for semantic/vector search only',
        'finding catalogue items related by meaning, synonyms, similar wording, or conceptual similarity',
        'looking for items like a topic even when exact keywords may differ',
        'searching for forms, Data Models, Data Classes, Data Elements, Terms, or metadata using semantic similarity'
    ],
    avoidWhen = [
        'ordinary catalogue searching where combined keyword plus semantic retrieval is appropriate; use mauro_search',
        'exact keyword syntax, quoted phrase matching, OR, or exclusion is required; use mauro_keyword_search',
        'reading a known resource URI or id; use mauro_get',
        'listing API resources; use mauro_list'
    ],
    examples = [
        'Find forms semantically related to diabetes => query "diabetes", domainTypes ["DataModel"]',
        'Find items like maternity care even if they use different words',
        'Find semantically similar Data Elements about smoking'
    ],
    filtering = [
        'use domainTypes to restrict result types, for example ["DataModel"], ["DataClass"], or ["DataElement"]',
        'use max and offset for returned-result paging',
        'use deepSearch only when the user explicitly wants broader semantic recall and accepts slower results'
    ],
    inputSchema = '{"type":"object","properties":{"query":{"type":"string","description":"Text to search by semantic similarity."},"searchTerm":{"type":"string","description":"Alias for query."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder"]},"description":"Optional catalogue result type filter."},"modelId":{"type":"string","format":"uuid","description":"Optional UUID of a DataModel, Terminology, CodeSet, Folder, or VersionedFolder to scope the search. Folder scopes include descendant folders and contained models."},"corpus":{"type":"string","description":"Optional API-visible semantic corpus name. Omit to search API-visible corpora for the requested model scope."},"embeddingProfiles":{"type":"array","items":{"type":"string"},"description":"Optional enabled embedding profile names to search. Omit to use all enabled profiles for the requested corpus/model scope."},"profileName":{"type":"string","description":"Alias for a single embedding profile name."},"max":{"type":"integer","minimum":1,"maximum":20,"description":"Maximum returned results. Omit for default page size."},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for paging."},"includeChunks":{"type":"boolean","description":"Whether to include matched evidence snippets. Defaults to true."},"deepSearch":{"type":"boolean","description":"When true, prioritise broader semantic recall over speed. Defaults to false."}}}'
)
class SemanticSearchToolHandler extends AbstractAnnotatedToolHandler {

    private static final int DEFAULT_PAGE_SIZE = 5
    private static final Set<String> ALLOWED_DOMAIN_TYPES = [
        'DataModel',
        'DataClass',
        'DataElement',
        'DataType',
        'EnumerationType',
        'EnumerationValue',
        'CodeSet',
        'Terminology',
        'Term',
        'Folder',
        'VersionedFolder'
    ] as Set<String>

    private final SemanticSearchService semanticSearchService
    private final AdministeredItemLookupService administeredItemLookupService

    SemanticSearchToolHandler(SemanticSearchService semanticSearchService,
                              AdministeredItemLookupService administeredItemLookupService) {
        super(SemanticSearchToolHandler)
        this.semanticSearchService = semanticSearchService
        this.administeredItemLookupService = administeredItemLookupService
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        long started = System.currentTimeMillis()
        String query = asString(arguments.get('query')) ?: asString(arguments.get('searchTerm'))
        if (query == null || query.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'mauro_semantic_search requires query')
        }

        SemanticSearchRequestDTO request = new SemanticSearchRequestDTO(
            query: query,
            searchTerm: query,
            corpus: asString(arguments.get('corpus') ?: arguments.get('corpusName')),
            domainTypes: extractStringList(arguments.get('domainTypes')).findAll {String domainType -> ALLOWED_DOMAIN_TYPES.contains(domainType)} as List<String>,
            withinModelId: asUuid(arguments.get('modelId') ?: arguments.get('withinModelId')),
            embeddingProfiles: embeddingProfiles(arguments),
            indexName: 'catalogue-items-default',
            topN: 50,
            topM: 10,
            max: Math.min(Math.max(asInteger(arguments.get('max'), DEFAULT_PAGE_SIZE), 1), 20),
            offset: asInteger(arguments.get('offset'), 0),
            includeChunks: asBoolean(arguments.get('includeChunks'), true),
            deepSearch: asBoolean(arguments.get('deepSearch'), false),
            rebuildIfEmpty: false
        )
        long requestBuiltAt = System.currentTimeMillis()
        ListResponse<SemanticSearchResultsDTO> response = semanticSearchService.executeSearch(
            request,
            { String domainType, UUID id -> administeredItemLookupService.findAdministeredItem(domainType, id) }
        )
        long searchCompletedAt = System.currentTimeMillis()
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>()
        for (SemanticSearchResultsDTO result : response.items ?: []) {
            items.add(itemToMap(result))
        }
        long itemsMappedAt = System.currentTimeMillis()
        Map<String, Object> timing = [
            requestBuildMs: requestBuiltAt - started,
            executeSearchMs: searchCompletedAt - requestBuiltAt,
            mapItemsMs: itemsMappedAt - searchCompletedAt,
            totalInvokeMs: itemsMappedAt - started
        ] as Map<String, Object>
        Map<String, Object> result = [
            query: query,
            corpus: request.corpus,
            domainTypes: request.domainTypes,
            modelId: request.withinModelId?.toString(),
            embeddingProfiles: request.embeddingProfiles,
            count: response.count ?: 0,
            countIsExact: response.countIsExact,
            max: request.max,
            offset: request.offset,
            nextOffset: request.offset + items.size(),
            hasMore: (request.offset + items.size()) < (response.count ?: 0),
            includeChunks: request.includeChunks,
            deepSearch: request.deepSearch,
            semanticAvailable: true,
            fallbackReason: null,
            telemetry: timing,
            items: items
        ] as Map<String, Object>
        log.info(
            'mauro_semantic_search invoke timing query="{}" modelId={} corpus={} count={} returned={} telemetry={}',
            query,
            request.withinModelId,
            request.corpus,
            response.count ?: 0,
            items.size(),
            timing
        )
        result
    }

    @Override
    String modelText(Map<String, Object> result) {
        long started = System.currentTimeMillis()
        List<?> items = result.get('items') instanceof List ? (List<?>) result.get('items') : []
        List<String> returnedData = new ArrayList<String>()
        int index = 1
        for (Object itemObj : items) {
            if (itemObj instanceof Map) {
                Map<?, ?> item = (Map<?, ?>) itemObj
                returnedData.add("${index}. ${item.get('label')} [${item.get('domainType')}] (id: ${item.get('id')}, semanticScore: ${item.get('semanticScore')})".toString())
                index++
            }
        }
        long returnedDataBuiltAt = System.currentTimeMillis()
        boolean countIsExact = result.get('countIsExact') != false
        String countDescription = countIsExact ?
            "${result.get('count')}".toString() :
            "at least ${result.get('count')}".toString()
        String text = renderModelTextSections([
            'Tool Call Status': ['Tool mauro_semantic_search succeeded.'],
            'Result Metadata': [
                "Query: ${result.get('query')}",
                "Matching semantic candidates: ${countDescription}",
                "Count is exact: ${countIsExact}",
                "Returned items for this page: ${items.size()}",
                "Semantic available: ${result.get('semanticAvailable')}",
                result.get('fallbackReason') ? "Semantic fallback reason: ${result.get('fallbackReason')}" : null,
                "Domain type filter: ${((List<?>) (result.get('domainTypes') ?: [])).join(', ')}",
                "Embedding profiles: ${((List<?>) (result.get('embeddingProfiles') ?: [])).join(', ')}",
                result.get('corpus') ? "Corpus: ${result.get('corpus')}" : null,
                result.get('modelId') ? "Model scope: ${result.get('modelId')}" : null,
                "Deep search: ${result.get('deepSearch')}",
                "Has more results: ${result.get('hasMore')}"
            ].findAll {Object value -> value != null},
            'Returned Data': returnedData ?: ['No semantic results were returned.'],
            'Answer Instructions': ([
                'Explain that this is semantic/vector-style similarity search, not exact keyword matching.',
                'Present returned matches as a Markdown table with Label, Type, ID, Semantic Score, Evidence.',
                'Use chunk evidence to explain why a result matched when available.',
                'If the user asks to inspect a returned DataModel, use mauro_get with the matching row ID.',
                'If exact keyword syntax matters, use mauro_keyword_search instead.'
            ].findAll {String instruction -> instruction != null})
        ] as Map<String, Object>)
        long renderedAt = System.currentTimeMillis()
        log.info(
            'mauro_semantic_search modelText timing query="{}" count={} returned={} telemetry={} timingsMs={returnedData={}, render={}, total={}}',
            result.get('query'),
            result.get('count'),
            items.size(),
            result.get('telemetry'),
            returnedDataBuiltAt - started,
            renderedAt - returnedDataBuiltAt,
            renderedAt - started
        )
        text
    }

    private static Map<String, Object> itemToMap(SemanticSearchResultsDTO result) {
        List<SemanticChunkMatchDTO> chunks = result.chunks ?: Collections.<SemanticChunkMatchDTO>emptyList()
        [
            id: result.id?.toString(),
            domainType: result.domainType,
            label: result.label,
            description: result.description,
            semanticScore: result.semanticScore,
            rerankScore: result.rerankScore,
            matchedChunkCount: result.matchedChunkCount,
            chunks: chunks.collect {SemanticChunkMatchDTO chunk ->
                [
                    chunkId: chunk.chunkId?.toString(),
                    kind: chunk.chunkKind,
                    ordinal: chunk.chunkOrdinal,
                    text: chunk.sourceText,
                    similarity: chunk.similarity
                ] as Map<String, Object>
            }
        ] as Map<String, Object>
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

    private static List<String> embeddingProfiles(Map<String, Object> arguments) {
        List<String> profiles = extractStringList(arguments.get('embeddingProfiles') ?: arguments.get('profiles'))
        String profileName = asString(arguments.get('profileName') ?: arguments.get('embeddingProfile'))
        if (profileName != null && !profileName.trim().isEmpty()) {
            profiles.add(profileName.trim())
        }
        profiles.unique() as List<String>
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
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        text.trim().isEmpty() ? fallback : Integer.valueOf(text)
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }
}
