package org.maurodata.service.chat.mcp

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.service.search.AdministeredItemLookupService
import org.maurodata.service.search.SearchExecutionService
import org.maurodata.web.ListResponse

@CompileStatic
@Singleton
class SearchToolHandler extends AbstractAnnotatedToolHandler {

    private final SearchExecutionService searchExecutionService
    private final AdministeredItemLookupService administeredItemLookupService
    private static final int DEFAULT_PAGE_SIZE = 10
    private static final int DEFAULT_MAX_PAGE_SIZE = 50

    SearchToolHandler(
        SearchExecutionService searchExecutionService,
        AdministeredItemLookupService administeredItemLookupService
    ) {
        super(SearchExecutionService)
        this.searchExecutionService = searchExecutionService
        this.administeredItemLookupService = administeredItemLookupService
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String searchTerm = extractSearchTerm(arguments)
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new HttpStatusException(
                HttpStatus.BAD_REQUEST,
                'catalogue_search requires searchTerm (or query/term/text)'
            )
        }

        SearchRequestDTO request = new SearchRequestDTO()
        request.searchTerm = searchTerm
        List<String> domainTypes = extractDomainTypes(arguments)
        request.domainTypes = domainTypes
        Integer requestedMax = asInteger(arguments.get('max')) ?: DEFAULT_PAGE_SIZE
        request.max = Math.min(Math.max(requestedMax, 1), DEFAULT_MAX_PAGE_SIZE)
        request.offset = asInteger(arguments.get('offset')) ?: 0
        boolean withGuidance = asBoolean(extractArgument(arguments, 'withGuidance'), true)
        String searchIntent = normalizeSearchIntent(asString(extractArgument(arguments, 'searchIntent')))

        ListResponse<SearchResultsDTO> response = searchExecutionService.executeSearch(
            request,
            { String domainType, UUID id -> administeredItemLookupService.findAdministeredItem(domainType, id) }
        )

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>()
        List<SearchResultsDTO> responseItems = response.items ?: []
        for (int i = 0; i < responseItems.size(); i++) {
            SearchResultsDTO result = responseItems.get(i)
            Map<String, Object> row = new LinkedHashMap<String, Object>()
            row.put('id', result.id?.toString())
            row.put('domainType', result.domainType)
            row.put('label', result.label)
            row.put('description', result.description)
            row.put('tsRank', result.tsRank)
            items.add(row)
        }

        [
            searchTerm: searchTerm,
            domainTypes: domainTypes,
            count: response.count ?: 0,
            max  : request.max,
            offset: request.offset,
            nextOffset: request.offset + items.size(),
            hasMore: (request.offset + items.size()) < (response.count ?: 0),
            withGuidance: withGuidance,
            searchIntent: searchIntent,
            items: items
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> items = result.get('items') instanceof List ? (List<?>) result.get('items') : Collections.emptyList()
        int totalCount = asInteger(result.get('count')) ?: items.size()
        int returnedCount = items.size()
        int offset = asInteger(result.get('offset')) ?: 0
        int max = asInteger(result.get('max')) ?: returnedCount
        int nextOffset = asInteger(result.get('nextOffset')) ?: offset + returnedCount
        boolean hasMore = Boolean.TRUE.equals(result.get('hasMore'))
        boolean withGuidance = !Boolean.FALSE.equals(result.get('withGuidance'))
        String searchTerm = asText(result.get('searchTerm'), '')
        String searchIntent = normalizeSearchIntent(asText(result.get('searchIntent'), ''))
        List<String> domainTypes = extractStringList(result.get('domainTypes'))
        boolean unexpandedKeywordSearch = isUnexpandedKeywordSearch(searchTerm, searchIntent, withGuidance, offset)
        List<String> orRetryTerms = extractOrRetryTerms(searchTerm)
        boolean zeroResultAndSearch = withGuidance && totalCount == 0 && offset == 0 && !orRetryTerms.isEmpty()
        int maxItems = returnedCount
        int pageSize = Math.max(max, 1)
        int pageNumber = offset.intdiv(pageSize) + 1
        int totalPages = Math.max((int) Math.ceil(totalCount / (double) pageSize), returnedCount > 0 ? 1 : 0)
        int rangeStart = returnedCount > 0 ? offset + 1 : 0
        int rangeEnd = offset + returnedCount

        List<String> status = [
            'Tool catalogue_search succeeded.'
        ]

        List<String> metadata = [
            "Search term: ${searchTerm}",
            "Total matching catalogue items: ${totalCount}",
            "Returned items for this page: ${returnedCount}",
            "Page: ${pageNumber} of ${totalPages}",
            "Visible range: ${rangeStart}-${rangeEnd} of ${totalCount}",
            "Page size: ${pageSize}",
            "Offset: ${offset}",
            "Has more results: ${hasMore}",
            "With guidance: ${withGuidance}",
            "Search intent: ${searchIntent}"
        ] as List<String>
        if (!domainTypes.isEmpty()) {
            metadata.add("Domain type filter: ${domainTypes.join(', ')}".toString())
        }

        List<String> instructions = new ArrayList<String>()
        instructions.add("Tell the user the exact catalogue search term/expression used: ${searchTerm}".toString())
        if (!domainTypes.isEmpty()) {
            instructions.add("Tell the user the domain type filter used: ${domainTypes.join(', ')}.".toString())
        }
        if (returnedCount > 0) {
            instructions.add('When presenting these catalogue_search results, follow these tool-owned formatting instructions rather than any general persona wording.')
            instructions.add("Tell the user the total number of matching catalogue items is ${totalCount}.".toString())
            instructions.add("Use this pagination summary in your answer: Page ${pageNumber} of ${totalPages}. Showing ${rangeStart}-${rangeEnd} of ${totalCount} matching catalogue items, ${pageSize} results at a time.".toString())
            instructions.add('Present the returned matches as a Markdown table. Use exactly these columns when possible: Label, Type, Description.')
            instructions.add('Keep descriptions short and leave the Description cell blank when no description is available.')
            instructions.add('Include each returned item from the Returned Data section unless the user explicitly asked for fewer results.')
            if (!domainTypes.isEmpty()) {
                instructions.add("Tell the user these results are filtered to ${domainTypes.join(', ')} only.".toString())
            }
            if (hasMore) {
                instructions.add('Explicitly say that more results are available and offer to show the next page.')
            }
        } else {
            instructions.add('Tell the user that this exact catalogue search returned no matching items.')
        }

        List<String> clarificationGuidance = new ArrayList<String>()
        if (unexpandedKeywordSearch) {
            clarificationGuidance.add('This looks like an exact keyword search using one unexpanded term.')
            clarificationGuidance.add("When asking the user how to proceed, explicitly state that the current search used searchTerm \"${searchTerm}\"".toString() + (domainTypes.isEmpty() ? '.' : " and domainTypes [${domainTypes.join(', ')}].".toString()))
            clarificationGuidance.add("Also state the current exact-term result count: ${returnedCount} returned on this page, ${totalCount} total matching catalogue items.".toString())
            clarificationGuidance.add('Still show the returned results from this page using the table format from Answer Instructions; the clarification question is an addition, not a replacement for the result list.')
            clarificationGuidance.add('Ask the user whether they want to keep the exact keyword search, or expand the keyword expression with related terms and alternatives.')
            clarificationGuidance.add('Do not silently choose the expanded keyword route for the user.')
            clarificationGuidance.add('If they choose the expanded keyword route, call related_terms for the current search term, then call catalogue_search with the expanded keyword expression and searchIntent "expanded".')
            clarificationGuidance.add('If they choose the exact keyword route, continue from the current exact-term result and use searchIntent "exact" for follow-up pages or refinements.')
            clarificationGuidance.add('Give the user an outcome in this turn: ask the focused choice question and briefly state that the current exact-term search has already returned results.')
        }
        if (zeroResultAndSearch) {
            clarificationGuidance.add('This exact search returned no matches and appears to require all supplied words to be present.')
            clarificationGuidance.add('Explain that catalogue_search is a keyword/full-text search. The backend treats unquoted words as AND terms, so this keyword expression may have been too narrow.')
            clarificationGuidance.add('Use the retry tool call in Follow-up Actions to search the same user-supplied terms as OR alternatives; do not rewrite the terms semantically and do not stop after reporting zero results.')
        }

        List<String> constraints = new ArrayList<String>()
        if (!domainTypes.isEmpty()) {
            constraints.add('Do not present items of other domain types as matches.')
        }
        if (hasMore) {
            constraints.add('Do not use only this returned page to answer aggregate questions about all matches.')
        }
        List<String> completionGuidance = [
            'If the returned data answers the current user request, answer now from this result.',
            'Do not call catalogue_search again with identical arguments.',
            'Only call catalogue_search again when the user asks for more results, requests a different page, or asks an aggregate/filtering follow-up about all matches.'
        ] as List<String>
        if (unexpandedKeywordSearch) {
            completionGuidance.add('Because Clarification Guidance is present, show the returned page results and then ask the focused choice question rather than presenting the search intent as settled.')
            completionGuidance.add('Even when asking that focused choice question, include the exact search term/expression and domain type filter used.')
        }
        if (zeroResultAndSearch) {
            completionGuidance.add('Because this exact keyword search returned zero results and Follow-up Actions contains an OR retry call, call catalogue_search again with that retry call rather than asking the user to reformulate.')
        }

        List<String> output = new ArrayList<String>()
        for (int i = 0; i < maxItems; i++) {
            Object itemObj = items.get(i)
            if (!(itemObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> item = (Map<String, Object>) itemObj
            StringBuilder line = new StringBuilder(256)
            line.append(i + 1)
                .append('. Label: ')
                .append(asText(item.get('label'), 'Untitled'))
                .append('; Type: ')
                .append(asText(item.get('domainType'), 'unknown type'))
            String description = asText(item.get('description'), '')
            if (!description.trim().isEmpty()) {
                line.append('; Description: ')
                    .append(description.replace('\n', ' ').trim())
            }
            String id = asText(item.get('id'), '')
            if (!id.trim().isEmpty()) {
                line.append('; ID: ')
                    .append(id)
            }
            output.add(line.toString())
        }
        if (returnedCount > maxItems) {
            output.add("Only the first ${maxItems} returned items are shown here.".toString())
        }

        List<String> followUp = new ArrayList<String>()
        if (hasMore) {
            Map<String, Object> nextPageToolCall = [
                name     : 'catalogue_search',
                arguments: [
                    searchTerm: searchTerm,
                    domainTypes: domainTypes,
                    max       : max,
                    offset    : nextOffset,
                    withGuidance: withGuidance,
                    searchIntent: searchIntent
                ] as Map<String, Object>
            ] as Map<String, Object>
            followUp.add('More results are available.')
            followUp.add('If the user asks for the next page, use this exact tool call: ' + JsonOutput.toJson(nextPageToolCall))
            followUp.add('Do not increase max or reset offset when the user asks for the next page.')
            followUp.add('If asking a clarifying question, also mention that additional result pages are available.')
        } else if (returnedCount < max) {
            followUp.add('There are no more pages for this exact search.')
            followUp.add('If the user wants more possibilities, try an expanded keyword expression rather than increasing the offset.')
        }
        if (zeroResultAndSearch) {
            Map<String, Object> retryCall = [
                name     : 'catalogue_search',
                arguments: [
                    searchTerm  : buildOrSearchTerm(orRetryTerms),
                    domainTypes : domainTypes,
                    max         : max,
                    offset      : 0,
                    withGuidance: withGuidance,
                    searchIntent  : 'expanded'
                ] as Map<String, Object>
            ] as Map<String, Object>
            followUp.add('The exact keyword search returned zero results and used only AND-style terms.')
            followUp.add('Retry with this exact OR keyword search call using the same user-supplied terms: ' + JsonOutput.toJson(retryCall))
        }

        renderModelTextSections([
            'Tool Call Status'   : status,
            'Result Metadata'    : metadata,
            'Returned Data'      : output,
            'Answer Instructions': instructions,
            'Clarification Guidance': clarificationGuidance,
            'Constraints'        : constraints,
            'Completion Guidance': completionGuidance,
            'Follow-up Actions'  : followUp
        ] as Map<String, Object>)
    }

    private static String extractSearchTerm(Map<String, Object> arguments) {
        if (arguments == null) {
            return null
        }
        String direct = firstNonBlank(
            asString(arguments.get('searchTerm')),
            asString(arguments.get('query')),
            asString(arguments.get('term')),
            asString(arguments.get('text')),
            asString(arguments.get('keywords'))
        )
        if (direct != null) {
            return direct
        }
        Object nestedParameters = arguments.get('parameters')
        if (nestedParameters instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedParameters
            String nestedValue = extractSearchTerm(nested)
            if (nestedValue != null) {
                return nestedValue
            }
        }
        Object nestedInput = arguments.get('input')
        if (nestedInput instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedInput
            String nestedValue = extractSearchTerm(nested)
            if (nestedValue != null) {
                return nestedValue
            }
        }
        null
    }

    private static Object extractArgument(Map<String, Object> arguments, String key) {
        if (arguments == null || key == null) {
            return null
        }
        if (arguments.containsKey(key)) {
            return arguments.get(key)
        }
        Object nestedParameters = arguments.get('parameters')
        if (nestedParameters instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedParameters
            Object value = extractArgument(nested, key)
            if (value != null) {
                return value
            }
        }
        Object nestedInput = arguments.get('input')
        if (nestedInput instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedInput
            Object value = extractArgument(nested, key)
            if (value != null) {
                return value
            }
        }
        null
    }

    private static List<String> extractDomainTypes(Map<String, Object> arguments) {
        if (arguments == null) {
            return Collections.emptyList()
        }

        List<String> values = new ArrayList<String>()
        addDomainTypeValues(values, arguments.get('domainTypes'))
        addDomainTypeValues(values, arguments.get('domainType'))
        addDomainTypeValues(values, arguments.get('types'))
        addDomainTypeValues(values, arguments.get('type'))
        addDomainTypeValues(values, arguments.get('itemTypes'))
        addDomainTypeValues(values, arguments.get('itemType'))
        addDomainTypeValues(values, arguments.get('filter'))

        Object nestedParameters = arguments.get('parameters')
        if (nestedParameters instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedParameters
            values.addAll(extractDomainTypes(nested))
        }
        Object nestedInput = arguments.get('input')
        if (nestedInput instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = (Map<String, Object>) nestedInput
            values.addAll(extractDomainTypes(nested))
        }

        List<String> normalized = new ArrayList<String>()
        for (String value : values) {
            String domainType = normalizeDomainType(value)
            if (domainType != null && !normalized.contains(domainType)) {
                normalized.add(domainType)
            }
        }
        normalized
    }

    private static List<String> extractStringList(Object value) {
        List<String> values = new ArrayList<String>()
        addDomainTypeValues(values, value)
        values.findAll {String text -> text != null && !text.trim().isEmpty()}
    }

    private static void addDomainTypeValues(List<String> values, Object value) {
        if (value == null) {
            return
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addDomainTypeValues(values, item)
            }
            return
        }
        if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                addDomainTypeValues(values, item)
            }
            return
        }
        String text = asString(value)
        if (text != null && !text.trim().isEmpty()) {
            values.add(text)
        }
    }

    private static String normalizeDomainType(String value) {
        if (value == null) {
            return null
        }
        String key = value.trim()
            .replaceAll(/([a-z])([A-Z])/, '$1 $2')
            .replaceAll(/[_-]+/, ' ')
            .replaceAll(/\s+/, ' ')
            .toLowerCase(Locale.ROOT)
        Map<String, String> aliases = [
            'data model'           : 'DataModel',
            'datamodel'            : 'DataModel',
            'data models'          : 'DataModel',
            'datamodels'           : 'DataModel',
            'model'                : 'DataModel',
            'models'               : 'DataModel',
            'form'                 : 'DataModel',
            'forms'                : 'DataModel',
            'data class'           : 'DataClass',
            'dataclass'            : 'DataClass',
            'data classes'         : 'DataClass',
            'dataclasses'          : 'DataClass',
            'class'                : 'DataClass',
            'classes'              : 'DataClass',
            'data element'         : 'DataElement',
            'dataelement'          : 'DataElement',
            'data elements'        : 'DataElement',
            'dataelements'         : 'DataElement',
            'element'              : 'DataElement',
            'elements'             : 'DataElement',
            'data type'            : 'DataType',
            'datatype'             : 'DataType',
            'data types'           : 'DataType',
            'datatypes'            : 'DataType',
            'enumeration type'     : 'EnumerationType',
            'enumerationtype'      : 'EnumerationType',
            'enumeration value'    : 'EnumerationValue',
            'enumerationvalue'     : 'EnumerationValue',
            'enum value'           : 'EnumerationValue',
            'code set'             : 'CodeSet',
            'codeset'              : 'CodeSet',
            'terminology'          : 'Terminology',
            'term'                 : 'Term',
            'folder'               : 'Folder',
            'versioned folder'     : 'VersionedFolder',
            'versionedfolder'      : 'VersionedFolder',
            'classification scheme': 'ClassificationScheme',
            'classificationscheme' : 'ClassificationScheme',
            'classifier'           : 'Classifier'
        ] as Map<String, String>
        aliases.get(key)
    }

    private static String firstNonBlank(String... values) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i]
            if (value != null && !value.trim().isEmpty()) {
                return value
            }
        }
        null
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static String asText(Object value, String fallback) {
        value == null ? fallback : String.valueOf(value)
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue()
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT)
        if (['true', 'yes', 'y', '1', 'on'].contains(text)) {
            return true
        }
        if (['false', 'no', 'n', '0', 'off'].contains(text)) {
            return false
        }
        fallback
    }

    private static String normalizeSearchIntent(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 'unsaid'
        }
        String intent = value.trim().toLowerCase(Locale.ROOT)
        if (['exact', 'expanded'].contains(intent)) {
            return intent
        }
        'unsaid'
    }

    private static boolean isUnexpandedKeywordSearch(String searchTerm, String searchIntent, boolean withGuidance, int offset) {
        if (!withGuidance || offset > 0) {
            return false
        }
        if (!'unsaid'.equals(normalizeSearchIntent(searchIntent))) {
            return false
        }
        String term = searchTerm == null ? '' : searchTerm.trim()
        if (term.isEmpty()) {
            return false
        }
        if ((term =~ /(?i)\bOR\b/).find()) {
            return false
        }
        true
    }

    private static List<String> extractOrRetryTerms(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return []
        }
        if ((searchTerm =~ /(?i)\bOR\b/).find()) {
            return []
        }
        String cleaned = searchTerm
            .replaceAll(/[(),;]+/, ' ')
            .replaceAll(/\s+/, ' ')
            .trim()
        if (cleaned.isEmpty()) {
            return []
        }
        List<String> terms = new ArrayList<String>()
        for (String token : cleaned.split(/\s+/)) {
            String term = token.trim()
            if (term.length() > 1 && !terms.contains(term)) {
                terms.add(term)
            }
        }
        terms.size() > 1 ? terms : []
    }

    private static String buildOrSearchTerm(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return ''
        }
        terms.collect {String term -> quoteIfNeededForSearch(term)}.join(' OR ')
    }

    private static String quoteIfNeededForSearch(String term) {
        if (term == null) {
            return ''
        }
        String cleaned = term.trim()
        cleaned.contains(' ') ? '"' + cleaned.replace('"', '\\"') + '"' : cleaned
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        if (text.trim().isEmpty()) {
            return null
        }
        Integer.valueOf(text)
    }
}
