package org.maurodata.service.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.ClassifierCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.search.SearchRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.chat.mcp.McpToolDefinition
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_keyword_search',
    description = 'Search stored Mauro catalogue content using PostgreSQL keyword/full-text matching only.',
    purpose = 'Run the narrower PostgreSQL full-text keyword search over stored Mauro catalogue content. For ordinary catalogue searches use mauro_search, which combines keyword search with semantic search when available. Use this specialist tool when exact keyword syntax or a previous keyword-only search must be preserved.',
    useWhen = [
        'the user explicitly asks for keyword/full-text search only',
        'exact PostgreSQL keyword syntax, quoted phrase matching, OR, or exclusion must be preserved exactly',
        'retrieving another page of a previous mauro_keyword_search result'
    ],
    avoidWhen = [
        'ordinary catalogue searching where combined keyword plus semantic retrieval is appropriate; use mauro_search',
        'answering Mauro installation, configuration, Docker, administration, or documentation/how-to questions',
        'answering general medical or domain knowledge questions rather than finding catalogue content',
        'interpreting ambiguous form language such as questions, fields, answers, sections, submissions, or records when the Mauro meaning is not yet clear; retrieve a relevant skill route first'
    ],
    examples = [
        'Find forms about diabetes => searchTerm "diabetes", domainTypes ["DataModel"]',
        'Find Data Models about risk assessments, list them, then inspect the first one => searchTerm "risk assessments", domainTypes ["DataModel"]; omit max so the first page can be listed, then use the first returned item id for mauro_get',
        'Search for "maternity care" Data Models',
        'List Data Elements about admission OR discharge',
        'Find forms and fields about diabetes => searchTerm "diabetes", domainTypes ["DataModel","DataElement"]',
        'List questions related to smoking => searchTerm "smoking", domainTypes ["DataElement"]'
    ],
    syntax = [
        'searchTerm uses PostgreSQL websearch_to_tsquery keyword syntax, not semantic/vector search',
        'the search backend treats unquoted words as AND terms; do not add extra required terms unless the user asked for all of them to be required',
        'quoted phrases preserve word order after text-search normalization',
        'OR expresses keyword alternatives',
        'a leading - excludes a term or quoted phrase',
        'use quoted phrases for exact labels or multi-word concepts, OR for synonyms/alternatives, and -term to remove noisy concepts'
    ],
    filtering = [
        'use domainTypes to restrict result types, for example ["DataModel"], ["DataClass"], or ["DataElement"]',
        'when searching for a specific item type, pass domainTypes explicitly',
        'when searching for forms or whole form templates about a topic, search actual catalogue content with domainTypes ["DataModel"]',
        'when searching for form questions, questions, fields, or form controls about a topic, search actual catalogue content with domainTypes ["DataElement"]'
    ],
    paging = [
        'use max for page size',
        'omit max for normal list/search requests so the default page is returned',
        'use max 1 only when the user asks for a single result/item/form/model; do not use max 1 merely because a later requested step refers to the first result after listing results',
        'use offset for subsequent pages',
        'when retrieving the next page, keep the same searchTerm, domainTypes, max, withGuidance, and searchIntent, and set offset to nextOffset from the previous result'
    ],
    limitations = [
        'not for Mauro installation, configuration, Docker, administration, or documentation/how-to questions'
    ],
    inputSchema = '{"type":"object","properties":{"searchTerm":{"type":"string","description":"Keyword search expression using PostgreSQL websearch_to_tsquery syntax. This is not semantic/vector search. The backend treats unquoted words as AND terms, quoted phrases preserve word order, OR expresses alternatives, and - excludes a term or quoted phrase. Preserve the user supplied keywords unless intentionally converting a comma/list of alternatives into OR. Examples: \\"maternity care\\", \\"\\\\\\"maternity care\\\\\\"\\", \\"diabetes OR diabetic\\", \\"diabetes -outpatients\\", \\"signal -\\\\\\"segmentation fault\\\\\\"\\"."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder","ClassificationScheme","Classifier"]},"description":"Optional catalogue domain type filter. Omit for no domain type filter. Use when the request clearly restricts results to a type, for example [\\"DataModel\\"] for Data Model results only, [\\"DataClass\\"] for Data Class results only, or [\\"DataElement\\"] for Data Element results only."},"modelId":{"type":"string","format":"uuid","description":"Optional UUID of a DataModel, Terminology, CodeSet, Folder, or VersionedFolder to scope the search. Folder scopes include descendant folders and contained models."},"max":{"type":"integer","minimum":1,"maximum":20,"description":"Optional maximum number of results to return for this page. Omit to use the default page size of 10; maximum is 20. Use max 1 only when the user asks for a single result; do not use max 1 merely because a later step asks to inspect the first item after listing results."},"offset":{"type":"integer","minimum":0,"description":"Optional zero-based offset for paging through additional results. Omit for the first page."},"withGuidance":{"type":"boolean","description":"Optional. When true, the tool may include guidance for the assistant to ask a focused follow-up question or carry out a follow-up workflow. Omit to use the default value true."},"searchIntent":{"type":"string","enum":["unsaid","exact","expanded"],"description":"Optional user intent for the supplied keyword expression. Omit to use unsaid. The search engine is always PostgreSQL full-text keyword search. unsaid means the user did not state exact versus expanded keyword matching. exact means use the supplied keywords as written. expanded means related terms or alternatives have already been included in searchTerm."}},"required":["searchTerm"]}'
)
class SearchExecutionService {

    private final SearchRepository searchRepository
    private final AccessControlService accessControlService
    private final PathRepository pathRepository
    private final ClassifierCacheableRepository classifierCacheableRepository

    SearchExecutionService(
        SearchRepository searchRepository,
        AccessControlService accessControlService,
        PathRepository pathRepository,
        ClassifierCacheableRepository classifierCacheableRepository
    ) {
        this.searchRepository = searchRepository
        this.accessControlService = accessControlService
        this.pathRepository = pathRepository
        this.classifierCacheableRepository = classifierCacheableRepository
    }

    ListResponse<SearchResultsDTO> executeSearch(
        SearchRequestDTO requestDTO,
        BiFunction<String, UUID, AdministeredItem> itemLookup
    ) {
        long startTime = System.currentTimeMillis()
        List<SearchResultsDTO> searchResults = retrieveSearchResults(requestDTO)
        log.debug('Search time taken (retrieve): {}', System.currentTimeMillis() - startTime)

        List<SearchResultsDTO> searchResultsReadable = filterReadable(searchResults, requestDTO, itemLookup)

        log.debug('Search time taken (retrieve + filter): {}', System.currentTimeMillis() - startTime)
        ListResponse.from(searchResultsReadable, requestDTO)
    }

    List<SearchResultsDTO> retrieveSearchResults(SearchRequestDTO requestDTO) {
        searchRepository.search(requestDTO)
    }

    List<SearchResultsDTO> filterReadable(
        List<SearchResultsDTO> searchResults,
        SearchRequestDTO requestDTO,
        BiFunction<String, UUID, AdministeredItem> itemLookup
    ) {
        Set<UUID> allClassifierIds = [] as Set<UUID>
        if (requestDTO.classifiers) {
            allClassifierIds.addAll(requestDTO.classifiers as Collection<UUID>)
        }
        List<Classifier> allClassifiers = classifierCacheableRepository.readAllByIdIn(allClassifierIds)
        Map<UUID, Set<UUID>> classifierMap = [:].withDefault { [] as Set<UUID> } as Map<UUID, Set<UUID>>
        allClassifiers.each { Classifier classifier ->
            classifierMap[classifier.classificationScheme.id] << classifier.id
        }

        List<SearchResultsDTO> searchResultsReadable = searchResults.findAll { SearchResultsDTO result ->
            AdministeredItem item = itemLookup.apply(result.domainType, result.id)
            if (!accessControlService.canDoRole(Role.READER, item)) {
                return false
            }
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
            result.classifiers = item.classifiers
            Set<UUID> resultClassifierIds = result.classifiers.id as Set<UUID>

            boolean classifierFilter = allClassifierIds.isEmpty() ||
                classifierMap.every { UUID ignored, Set<UUID> classifiers ->
                    classifiers.any { UUID cid -> resultClassifierIds.contains(cid) }
                }
            classifierFilter
        } as List<SearchResultsDTO>
    }
}
