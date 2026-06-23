package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
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
@McpToolDefinition(
    name = 'mauro_semantic_search',
    description = 'Search Mauro catalogue content by semantic similarity using embedding profiles.',
    purpose = 'Find catalogue items by meaning/similarity rather than only PostgreSQL full-text keyword matches.',
    useWhen = [
        'finding catalogue items related by meaning, synonyms, similar wording, or conceptual similarity',
        'looking for items like a topic even when exact keywords may differ',
        'searching for forms, Data Models, Data Classes, Data Elements, Terms, or metadata using semantic similarity'
    ],
    avoidWhen = [
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
        'use topN for candidate retrieval and topM/max for final returned results',
        'embeddingProfiles can restrict which embedding profiles are used'
    ],
    inputSchema = '{"type":"object","properties":{"query":{"type":"string","description":"Text to embed and search semantically."},"searchTerm":{"type":"string","description":"Alias for query."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder","ClassificationScheme","Classifier"]},"description":"Optional catalogue domain type filter."},"embeddingProfiles":{"type":"array","items":{"type":"string"},"description":"Optional embedding profile names to use. Omit to use the semantic index defaults."},"indexName":{"type":"string","description":"Optional semantic index name. Omit for catalogue-items-default."},"topN":{"type":"integer","minimum":1,"maximum":500,"description":"Candidate chunks to retrieve per profile. Omit for 50."},"topM":{"type":"integer","minimum":1,"maximum":100,"description":"Final item candidates to keep before paging. Omit for 10."},"max":{"type":"integer","minimum":1,"maximum":20,"description":"Maximum returned results. Omit for 10."},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for paging."},"includeChunks":{"type":"boolean","description":"Whether to include matched chunk evidence. Defaults to true."},"rebuildIfEmpty":{"type":"boolean","description":"When true, rebuild the default catalogue semantic index if no embeddings exist. Use sparingly."}}}'
)
class SemanticSearchToolHandler extends AbstractAnnotatedToolHandler {

    private static final int DEFAULT_PAGE_SIZE = 5

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
        String query = asString(arguments.get('query')) ?: asString(arguments.get('searchTerm'))
        if (query == null || query.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'mauro_semantic_search requires query')
        }

        SemanticSearchRequestDTO request = new SemanticSearchRequestDTO(
            query: query,
            searchTerm: query,
            domainTypes: extractStringList(arguments.get('domainTypes')),
            embeddingProfiles: extractStringList(arguments.get('embeddingProfiles')),
            indexName: asString(arguments.get('indexName')) ?: 'catalogue-items-default',
            topN: asInteger(arguments.get('topN'), 50),
            topM: asInteger(arguments.get('topM'), 10),
            max: Math.min(Math.max(asInteger(arguments.get('max'), DEFAULT_PAGE_SIZE), 1), 20),
            offset: asInteger(arguments.get('offset'), 0),
            includeChunks: asBoolean(arguments.get('includeChunks'), true),
            rebuildIfEmpty: asBoolean(arguments.get('rebuildIfEmpty'), false)
        )
        SemanticSearchAvailability availability = semanticSearchService.availability(request.indexName)
        ListResponse<SemanticSearchResultsDTO> response = semanticSearchService.executeSearch(
            request,
            { String domainType, UUID id -> administeredItemLookupService.findAdministeredItem(domainType, id) }
        )
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>()
        for (SemanticSearchResultsDTO result : response.items ?: []) {
            items.add(itemToMap(result))
        }
        [
            query: query,
            domainTypes: request.domainTypes,
            indexName: request.indexName,
            embeddingProfiles: request.embeddingProfiles,
            count: response.count ?: 0,
            max: request.max,
            offset: request.offset,
            nextOffset: request.offset + items.size(),
            hasMore: (request.offset + items.size()) < (response.count ?: 0),
            includeChunks: request.includeChunks,
            semanticAvailable: availability.available,
            fallbackReason: availability.reason,
            items: items
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> items = result.get('items') instanceof List ? (List<?>) result.get('items') : []
        List<String> profiles = embeddingProfiles(items)
        List<String> returnedData = new ArrayList<String>()
        int index = 1
        for (Object itemObj : items) {
            if (itemObj instanceof Map) {
                Map<?, ?> item = (Map<?, ?>) itemObj
                returnedData.add("${index}. ${item.get('label')} [${item.get('domainType')}] (id: ${item.get('id')}, semanticScore: ${item.get('semanticScore')})".toString())
                index++
            }
        }
        renderModelTextSections([
            'Tool Call Status': ['Tool mauro_semantic_search succeeded.'],
            'Result Metadata': [
                "Query: ${result.get('query')}",
                "Total matching semantic candidates: ${result.get('count')}",
                "Returned items for this page: ${items.size()}",
                "Index: ${result.get('indexName')}",
                "Semantic available: ${result.get('semanticAvailable')}",
                result.get('fallbackReason') ? "Semantic fallback reason: ${result.get('fallbackReason')}" : null,
                "Embedding profiles used: ${profiles.join(', ')}",
                "Domain type filter: ${((List<?>) (result.get('domainTypes') ?: [])).join(', ')}",
                "Has more results: ${result.get('hasMore')}"
            ].findAll {Object value -> value != null},
            'Returned Data': returnedData ?: ['No semantic results were returned.'],
            'Answer Instructions': ([
                'Explain that this is semantic/vector-style similarity search, not exact keyword matching.',
                profiles.any {String profile -> profile.startsWith('test-')} ? 'The active embedding profile is test-only hash machinery, so do not claim domain-semantic quality for these rankings.' : null,
                'Present returned matches as a Markdown table with Label, Type, ID, Semantic Score, Evidence.',
                'Use chunk evidence to explain why a result matched when available.',
                'If the user asks to inspect a returned DataModel, use mauro_get with the matching row ID.',
                'If exact keyword syntax matters, use mauro_keyword_search instead.'
            ].findAll {String instruction -> instruction != null})
        ] as Map<String, Object>)
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
            embeddingProfiles: result.embeddingProfiles,
            chunks: chunks.collect {SemanticChunkMatchDTO chunk ->
                [
                    chunkId: chunk.chunkId?.toString(),
                    kind: chunk.chunkKind,
                    ordinal: chunk.chunkOrdinal,
                    text: chunk.sourceText,
                    profile: chunk.embeddingProfile,
                    similarity: chunk.similarity
                ] as Map<String, Object>
            }
        ] as Map<String, Object>
    }

    private static List<String> embeddingProfiles(List<?> items) {
        List<String> profiles = new ArrayList<String>()
        for (Object itemObj : items) {
            if (itemObj instanceof Map) {
                Object value = ((Map<?, ?>) itemObj).get('embeddingProfiles')
                if (value instanceof Collection) {
                    for (Object profile : (Collection<?>) value) {
                        String profileName = String.valueOf(profile)
                        if (profileName && !profiles.contains(profileName)) {
                            profiles.add(profileName)
                        }
                    }
                }
            }
        }
        profiles
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

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
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
