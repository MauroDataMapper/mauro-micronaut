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
    name = 'catalogue_search',
    description = 'Search stored Mauro catalogue content and return matching catalogue items.',
    purpose = 'Run a PostgreSQL full-text keyword search over stored Mauro catalogue content such as forms/Data Models, Data Classes, Data Elements, Terms, metadata, and other catalogue items. This is not a semantic/vector search.',
    useWhen = [
        'users ask to find, search, list, or inspect live catalogue content in the connected Mauro instance',
        'users ask for forms, Data Models, Data Classes, Data Elements, Terms, metadata, or other catalogue items about a subject',
        'users ask for another page of a previous catalogue_search result'
    ],
    avoidWhen = [
        'users ask for Mauro installation, configuration, Docker, administration, or documentation/how-to help',
        'users ask for general medical or domain knowledge rather than catalogue content',
        'users use ambiguous form language such as questions, fields, answers, sections, submissions, or records and the Mauro meaning is not yet clear; retrieve a relevant skill route first'
    ],
    examples = [
        'Find forms about diabetes => searchTerm "diabetes", domainTypes ["DataModel"]',
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
        'when the user asks for a specific item type, pass domainTypes explicitly',
        'when the user asks for forms or whole form templates about a topic, search actual catalogue content with domainTypes ["DataModel"]',
        'when the user asks for form questions, questions, fields, or form controls about a topic, search actual catalogue content with domainTypes ["DataElement"]'
    ],
    paging = [
        'use max for page size',
        'use offset for subsequent pages',
        'when the user asks for the next page, keep the same searchTerm, domainTypes, max, withGuidance, and searchIntent, and set offset to nextOffset from the previous result'
    ],
    limitations = [
        'not for Mauro installation, configuration, Docker, administration, or documentation/how-to questions'
    ],
    inputSchema = '{"type":"object","properties":{"searchTerm":{"type":"string","description":"Keyword search expression using PostgreSQL websearch_to_tsquery syntax. This is not semantic/vector search. The backend treats unquoted words as AND terms, quoted phrases preserve word order, OR expresses alternatives, and - excludes a term or quoted phrase. Preserve the user supplied keywords unless intentionally converting a comma/list of alternatives into OR. Examples: \\"maternity care\\", \\"\\\\\\"maternity care\\\\\\"\\", \\"diabetes OR diabetic\\", \\"diabetes -outpatients\\", \\"signal -\\\\\\"segmentation fault\\\\\\"\\"."},"domainTypes":{"type":"array","items":{"type":"string","enum":["DataModel","DataClass","DataElement","DataType","EnumerationType","EnumerationValue","CodeSet","Terminology","Term","Folder","VersionedFolder","ClassificationScheme","Classifier"]},"description":"Optional catalogue domain type filter. Use when the user asks to restrict results to a type, for example [\\"DataModel\\"] for Data Model results only, [\\"DataClass\\"] for Data Class results only, or [\\"DataElement\\"] for Data Element results only."},"max":{"type":"integer","minimum":1,"maximum":200,"description":"Maximum number of results to return for this page"},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for paging through additional results"},"withGuidance":{"type":"boolean","description":"When true, the tool may include guidance for the assistant to ask a focused follow-up question or carry out a follow-up workflow. Defaults to true."},"searchIntent":{"type":"string","enum":["unsaid","exact","expanded"],"description":"The user intent for the supplied keyword expression. The search engine is always PostgreSQL full-text keyword search. unsaid means the user did not state exact versus expanded keyword matching. exact means use the supplied keywords as written. expanded means related terms or alternatives have already been included in searchTerm."}},"required":["searchTerm"]}'
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
        List<SearchResultsDTO> searchResults = searchRepository.search(requestDTO)
        log.debug('Search time taken (retrieve): {}', System.currentTimeMillis() - startTime)

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
        }

        log.debug('Search time taken (retrieve + filter): {}', System.currentTimeMillis() - startTime)
        ListResponse.from(searchResultsReadable, requestDTO)
    }
}
