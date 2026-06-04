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
        Integer requestedMax = asInteger(arguments.get('max')) ?: 20
        request.max = Math.min(Math.max(requestedMax, 1), 50)
        request.offset = asInteger(arguments.get('offset')) ?: 0

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
        String searchTerm = asText(result.get('searchTerm'), '')
        List<String> domainTypes = extractStringList(result.get('domainTypes'))
        int maxItems = Math.min(returnedCount, 10)
        int pageSize = Math.max(max, 1)
        int pageNumber = offset.intdiv(pageSize) + 1
        int totalPages = Math.max((int) Math.ceil(totalCount / (double) pageSize), returnedCount > 0 ? 1 : 0)
        int rangeStart = returnedCount > 0 ? offset + 1 : 0
        int rangeEnd = offset + returnedCount

        StringBuilder builder = new StringBuilder(1024)
        builder.append('Tool catalogue_search succeeded. It found ')
            .append(totalCount)
            .append(' matching catalogue items')
        if (!domainTypes.isEmpty()) {
            builder.append(' filtered to domain type')
                .append(domainTypes.size() == 1 ? ' ' : 's ')
                .append(domainTypes.join(', '))
                .append('. Tell the user these results are filtered to ')
                .append(domainTypes.join(', '))
                .append(' only, and do not present items of other types as matches')
        }
        if (returnedCount > 0) {
            builder.append(' and returned ')
                .append(returnedCount)
                .append(' for this page (offset ')
                .append(offset)
                .append(', max ')
                .append(max)
                .append('). Tell the user the total number of matching catalogue items is ')
                .append(totalCount)
                .append('. Use this pagination summary in your answer: Page ')
                .append(pageNumber)
                .append(' of ')
                .append(totalPages)
                .append('. Showing ')
                .append(rangeStart)
                .append('-')
                .append(rangeEnd)
                .append(' of ')
                .append(totalCount)
                .append(' matching catalogue items, ')
                .append(pageSize)
                .append(' results at a time')
                .append('. Present the returned matches as a Markdown table. Use exactly these columns when possible: Label, Type, ID, Description. Keep descriptions short and leave the Description cell blank when no description is available.')
            if (hasMore) {
                builder.append(' In your answer, explicitly say that more results are available and offer to show the next page. Do not use only this returned page to answer aggregate questions about all matches.')
            }
            builder.append(' Do not call the same tool again for this request unless the user asks for more results.')
        }
        builder.append('.\n')

        for (int i = 0; i < maxItems; i++) {
            Object itemObj = items.get(i)
            if (!(itemObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> item = (Map<String, Object>) itemObj
            builder.append(i + 1)
                .append('. ')
                .append(asText(item.get('label'), 'Untitled'))
                .append(' [')
                .append(asText(item.get('domainType'), 'unknown type'))
                .append(']')
            String description = asText(item.get('description'), '')
            if (!description.trim().isEmpty()) {
                builder.append(' - ')
                    .append(description.replace('\n', ' ').trim())
            }
            String id = asText(item.get('id'), '')
            if (!id.trim().isEmpty()) {
                builder.append(' (id: ')
                    .append(id)
                    .append(')')
            }
            builder.append('\n')
        }
        if (returnedCount > maxItems) {
            builder.append('Only the first ')
                .append(maxItems)
                .append(' returned items are shown here.')
                .append('\n')
        }
        if (hasMore) {
            Map<String, Object> nextPageToolCall = [
                name     : 'catalogue_search',
                arguments: [
                    searchTerm: searchTerm,
                    domainTypes: domainTypes,
                    max       : max,
                    offset    : nextOffset
                ] as Map<String, Object>
            ] as Map<String, Object>
            builder.append('More results are available. Tell the user you can show more results. If the user asks for the next page, use this exact tool call: ')
                .append(JsonOutput.toJson(nextPageToolCall))
                .append('. Do not increase max or reset offset when the user asks for the next page. If asking a clarifying question, also mention that additional result pages are available.')
                .append('\n')
        } else if (returnedCount < max) {
            builder.append('There are no more pages for this exact search. If the user wants more possibilities, try a broader or related search term rather than increasing the offset.')
                .append('\n')
        }
        builder.toString()
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
