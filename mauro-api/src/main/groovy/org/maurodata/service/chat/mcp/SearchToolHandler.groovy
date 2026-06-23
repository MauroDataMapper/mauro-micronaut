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
    private static final int DEFAULT_PAGE_SIZE = 5
    private static final int DEFAULT_MAX_PAGE_SIZE = 20

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
                'mauro_keyword_search requires searchTerm (or query/term/text)'
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
            'Tool mauro_keyword_search succeeded.'
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
        instructions.add("COMMON: Tell the user the exact catalogue search term/expression used: ${searchTerm}".toString())
        if (!domainTypes.isEmpty()) {
            instructions.add("COMMON: Tell the user the domain type filter used: ${domainTypes.join(', ')}.".toString())
        }
        if (returnedCount > 0) {
            instructions.add('COMMON: When presenting these mauro_keyword_search results, follow these tool-owned formatting instructions rather than any general persona wording.')
            instructions.add("COMMON: Tell the user the total number of matching catalogue items is ${totalCount}.".toString())
            instructions.add("COMMON: Use this pagination summary in your answer: Page ${pageNumber} of ${totalPages}. Showing ${rangeStart}-${rangeEnd} of ${totalCount} matching catalogue items, ${pageSize} results at a time.".toString())
            instructions.add('COMMON: Present the returned matches as a Markdown table. Use exactly these columns when possible: Label, Type, ID, Description.')
            instructions.add('COMMON: Escape pipe characters inside Markdown table cell values as \\|.')
            instructions.add('COMMON: Keep descriptions short and leave the Description cell blank when no description is available.')
            instructions.add('COMMON: Include each returned item from the Returned Data section unless the user explicitly asked for fewer results.')
            instructions.add('COMMON: When the user refers to an ordinal result such as the first one, second one, or third one, use the ID from the matching numbered Returned Data item or Available Next Action. Do not guess or copy an ID from a different row.')
            instructions.add('COMMON: When the user later names one of these returned labels, use the ID from the same Returned Data row as that exact label. Verify the label and ID together before calling mauro_get.')
            instructions.add('COMMON: If you cannot find the named label in the visible Returned Data rows, call mauro_keyword_search again for that label and the appropriate domainTypes rather than reusing an unrelated ID.')
            if (!domainTypes.isEmpty()) {
                instructions.add("COMMON: Tell the user these results are filtered to ${domainTypes.join(', ')} only.".toString())
            }
        } else {
            instructions.add('COMMON: Tell the user that this exact catalogue search returned no matching items.')
        }

        List<String> continuationGate = [
            'Choose exactly one branch flag for this tool result: CW or FR.',
            'CW = Continue Workflow. Choose CW when the current user request has remaining planned/requested steps and an exact Available Next Action or Follow-up Action can complete the next unfinished requested step.',
            'FR = Final Response. Choose FR when the current user request is complete, or when no exact Available Next Action or Follow-up Action can complete the next unfinished requested step.',
            'If unsure, introspect privately: list the requested actions from the current user request and mark each as complete or incomplete.',
            'After choosing CW or FR, follow COMMON instructions and the instructions with the chosen flag. Ignore instructions with the other flag.'
        ] as List<String>

        List<String> continueWorkflowGuidance = [
            'CW: Present any required current result briefly if needed, then execute the exact matching action in this turn.',
            'CW: Use this branch to complete the next unfinished requested step; keep moving through the user\'s requested workflow.',
            'CW: Treat the current user request as sufficient confirmation for its requested steps.',
            'CW: Phrases such as "then take a closer look at the first one" are requested actions, not optional actions to offer.',
            'CW: Continue by calling the exact action rather than presenting that action as a user choice.',
            'CW: Skip FR instructions, pagination offers, refinement choices, and search-expansion questions while an exact action can complete an unfinished requested step.'
        ] as List<String>

        List<String> finalResponseGuidance = new ArrayList<String>()
        finalResponseGuidance.add('FR: Answer the user from this result when the requested workflow is complete or cannot be continued by an exact action.')
        if (hasMore) {
            finalResponseGuidance.add('FR: Say that more results are available and that you can show the next page.')
        }
        if (unexpandedKeywordSearch) {
            finalResponseGuidance.add('FR: This looks like an exact keyword search using one unexpanded term.')
            finalResponseGuidance.add("FR: For a final response that asks about search expansion, state that the current search used searchTerm \"${searchTerm}\"".toString() + (domainTypes.isEmpty() ? '.' : " and domainTypes [${domainTypes.join(', ')}].".toString()))
            finalResponseGuidance.add("FR: Also state the current exact-term result count: ${returnedCount} returned on this page, ${totalCount} total matching catalogue items.".toString())
            finalResponseGuidance.add('FR: Show the returned results from this page using the table format from Answer Instructions; any search-expansion question is an addition, not a replacement for the result list.')
            finalResponseGuidance.add('FR: For search expansion, ask for a focused choice between keeping the exact keyword search and expanding the keyword expression with related terms and alternatives.')
            finalResponseGuidance.add('FR: Use the expanded keyword route only after the user chooses expansion.')
            finalResponseGuidance.add('FR: If they choose the expanded keyword route, call mauro_terms for the current search term, then call mauro_keyword_search with the expanded keyword expression and searchIntent "expanded".')
            finalResponseGuidance.add('FR: If they choose the exact keyword route, continue from the current exact-term result and use searchIntent "exact" for follow-up pages or refinements.')
            finalResponseGuidance.add('FR: Give the user an outcome: show the returned page results and ask the focused search-expansion choice question.')
        }
        if (zeroResultAndSearch) {
            continueWorkflowGuidance.add('CW: Use the OR retry Follow-up Action to search the same user-supplied terms as alternatives.')
            finalResponseGuidance.add('FR: This exact search returned no matches and appears to require all supplied words to be present.')
            finalResponseGuidance.add('FR: Explain that mauro_keyword_search is a keyword/full-text search. The backend treats unquoted words as AND terms, so this keyword expression may have been too narrow.')
        }

        List<String> constraints = new ArrayList<String>()
        if (!domainTypes.isEmpty()) {
            constraints.add('COMMON: Do not present items of other domain types as matches.')
        }
        if (hasMore) {
            constraints.add('COMMON: Do not use only this returned page to answer aggregate questions about all matches.')
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
                name     : 'mauro_keyword_search',
                arguments: [
                    searchTerm: searchTerm,
                    domainTypes: domainTypes,
                    max       : max,
                    offset    : nextOffset,
                    withGuidance: withGuidance,
                    searchIntent: searchIntent
                ] as Map<String, Object>
            ] as Map<String, Object>
            followUp.add('FR: More results are available.')
            followUp.add('FR: In a later turn, if the user asks for the next page, use this exact tool call: ' + JsonOutput.toJson(nextPageToolCall))
            followUp.add('FR: Do not increase max or reset offset when the user asks for the next page.')
            followUp.add('FR: If asking a clarifying question, also mention that additional result pages are available.')
        } else if (returnedCount < max) {
            followUp.add('FR: There are no more pages for this exact search.')
            followUp.add('FR: If the user wants more possibilities, try an expanded keyword expression rather than increasing the offset.')
        }
        if (zeroResultAndSearch) {
            Map<String, Object> retryCall = [
                name     : 'mauro_keyword_search',
                arguments: [
                    searchTerm  : buildOrSearchTerm(orRetryTerms),
                    domainTypes : domainTypes,
                    max         : max,
                    offset      : 0,
                    withGuidance: withGuidance,
                    searchIntent  : 'expanded'
                ] as Map<String, Object>
            ] as Map<String, Object>
            followUp.add('CW: The exact keyword search returned zero results and used only AND-style terms.')
            followUp.add('CW: Retry with this exact OR keyword search call using the same user-supplied terms: ' + JsonOutput.toJson(retryCall))
        }
        List<String> end = [
            'END: Use COMMON plus exactly one branch flag: CW or FR.',
            'END: If CW is chosen, continue with an exact tool action as work you perform for the user.',
            'END: If FR is chosen, provide the final user-facing answer and mention only relevant optional follow-up actions.'
        ] as List<String>
        renderModelTextSections([
            'Tool Call Status'   : status,
            'Result Metadata'    : metadata,
            'Returned Data'      : output,
            'Answer Instructions': instructions,
            'Continuation Gate'   : continuationGate,
            'Continue Workflow branch': continueWorkflowGuidance,
            'Final Response branch': finalResponseGuidance,
            'Constraints'        : constraints,
            'Follow-up Actions'  : followUp,
            'END'                : end
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
