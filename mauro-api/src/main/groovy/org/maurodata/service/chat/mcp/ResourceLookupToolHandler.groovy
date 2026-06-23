package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_describe',
    description = 'Describe Mauro API resource types, supported operations, route templates, filters, affordances, and schema hints.',
    purpose = 'Discover what Mauro API operations are available before using mauro_list, mauro_get, mauro_schema, or gated write tools.',
    useWhen = [
        'finding supported operations for a Mauro API resource type',
        'checking whether a Data Model, Data Class, Data Element, Terminology, Code Set, or other Mauro API resource can be listed, read, created, updated, or deleted',
        'locating a resource template before calling mauro_get or mauro_schema',
        'inspecting available filters, identifiers, route templates, and next affordances'
    ],
    avoidWhen = [
        'searching unknown catalogue content by label or keyword; use mauro_keyword_search first',
        'reading a known resource URI; use mauro_get directly',
        'fetching body details for a known operation; use mauro_schema'
    ],
    examples = [
        'describe DataModel',
        'what operations are supported for Terminology?',
        'what can I do with DataClass resources?',
        'find create operations for DataModel',
        'show route templates and schema hints for CodeSet'
    ],
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Optional Mauro resource type to describe, for example DataModel, DataClass, DataElement, Terminology, CodeSet, Folder, Classifier."},"operation":{"type":"string","enum":["list","get","create","update","delete","other"],"description":"Optional operation family to filter by."},"query":{"type":"string","description":"Optional text to filter operation names, paths, descriptions, controllers, methods, operation ids, or summaries."},"templatesOnly":{"type":"boolean","description":"When true, return only parameterised route templates"},"max":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum operations to return. Omit for 20."}}}'
)
class ResourceLookupToolHandler extends AbstractAnnotatedToolHandler {

    private final McpHttpResourceRegistry resourceRegistry

    ResourceLookupToolHandler(McpHttpResourceRegistry resourceRegistry) {
        super(ResourceLookupToolHandler)
        this.resourceRegistry = resourceRegistry
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String resourceType = asString(arguments.get('resourceType'))
        String operation = asString(arguments.get('operation'))
        String query = asString(arguments.get('query'))
        boolean templatesOnly = asBoolean(arguments.get('templatesOnly'), false)
        int max = Math.max(1, Math.min(asInteger(arguments.get('max'), 20), 100))
        List<McpHttpResourceRegistry.McpHttpOperation> operations = new ArrayList<McpHttpResourceRegistry.McpHttpOperation>()
        operations.addAll(resourceRegistry.listOperations(resourceType, operation))
        if (templatesOnly) {
            operations = operations.findAll {McpHttpResourceRegistry.McpHttpOperation candidate -> candidate.template}
        }
        if (query != null && !query.trim().isEmpty()) {
            String lower = query.toLowerCase(Locale.ROOT)
            operations = operations.findAll {McpHttpResourceRegistry.McpHttpOperation candidate ->
                [
                    candidate.name,
                    candidate.path,
                    candidate.description,
                    candidate.controller,
                    candidate.method,
                    candidate.operationId,
                    candidate.summary,
                    candidate.resourceType,
                    candidate.operationKind
                ].any {String value -> value != null && value.toLowerCase(Locale.ROOT).contains(lower)}
            } as List<McpHttpResourceRegistry.McpHttpOperation>
        }
        [
            resourceType : resourceType,
            operation    : operation,
            query        : query,
            templatesOnly: templatesOnly,
            count        : operations.size(),
            max          : max,
            operations    : operations.take(max).collect {McpHttpResourceRegistry.McpHttpOperation candidate -> operationToMap(candidate)},
            operationFamilies: operationFamilies(operations),
            resourceTypes : resourceTypes(resourceRegistry.listOperations()),
            noMatchGuidance: noMatchGuidance(query ?: resourceType, operations.size())
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> operations = result.get('operations') instanceof List ? (List<?>) result.get('operations') : []
        List<?> noMatchGuidance = result.get('noMatchGuidance') instanceof List ? (List<?>) result.get('noMatchGuidance') : []
        List<String> rows = new ArrayList<String>()
        for (Object obj : operations) {
            if (obj instanceof Map) {
                Map<?, ?> operation = (Map<?, ?>) obj
                rows.add("${operation.get('resourceType')} | ${operation.get('operation')} | ${operation.get('name')} | ${operation.get('method')} ${operation.get('path')} | ${operation.get('description')}".toString())
            }
        }
        renderModelTextSections([
            'Tool Call Status'   : ['Tool mauro_describe succeeded.'],
            'Result Metadata'    : [
                "Resource type: ${result.get('resourceType') ?: ''}",
                "Operation filter: ${result.get('operation') ?: ''}",
                "Query: ${result.get('query') ?: ''}",
                "Matched operations: ${result.get('count')}",
                "Returned operations: ${operations.size()}",
                "Operation families in result: ${result.get('operationFamilies') ?: [:]}"
            ],
            'Returned Data'      : rows,
            'No Match Guidance'  : noMatchGuidance,
            'Affordances'        : [
                'Use mauro_list for concrete list operations.',
                'Use mauro_get for known HTTP GET resource URIs such as mauro-api://http-get/api/dataModels/{id} after replacing {id}.',
                'Use mauro_schema to inspect parameters and request body shape for a selected operation before create or update.',
                'Use mauro_create, mauro_update, or mauro_delete only for operations described as create, update, or delete, and only with explicit confirmation.'
            ],
            'Interpretation Hints': [
                'The operation description describes the HTTP route, not the content description of a returned catalogue item.',
                'Operation kind is derived from HTTP method plus controller method/OpenAPI operation metadata.',
                'Schema hints are route signatures, not exhaustive domain validation rules unless mauro_schema says an exact JSON schema is available.'
            ],
            'Answer Instructions': [
                'Present useful operation choices clearly, including resource type, operation, route, and method.',
                'If the user needs data from a known GET resource and required URI values are known, call mauro_get next.',
                'If the operation route is a template and required values are not known, ask a focused question for the missing value.',
                'If a previous mauro_keyword_search result already contains the required id and domain type, use that id to call mauro_get rather than asking the user to confirm it.'
            ]
        ] as Map<String, Object>)
    }

    private static List<String> noMatchGuidance(String query, int count) {
        if (count > 0 || query == null || query.trim().isEmpty()) {
            return Collections.<String>emptyList()
        }
        [
            'mauro_describe searches Mauro API operation names and URI templates; it does not search catalogue item labels.',
            'If the query is a catalogue item label, call mauro_keyword_search first to find the item id and domain type.',
            'If the conversation already contains a DataModel id, read it directly with mauro_get using URI mauro-api://http-get/api/dataModels/{id} after replacing {id}.',
            'For a known DataModel id, do not call mauro_describe again just to rediscover the route.'
        ] as List<String>
    }

    private static Map<String, Object> operationToMap(McpHttpResourceRegistry.McpHttpOperation operation) {
        [
            resourceType : operation.resourceType,
            operation    : operation.operationKind,
            name         : operation.name,
            uri          : operation.httpMethod == 'GET' ? McpHttpResourceRegistry.URI_PREFIX + operation.path : operation.operationUri,
            operationUri : operation.operationUri,
            method       : operation.httpMethod,
            path         : operation.path,
            template     : operation.template,
            pathParameters: operation.pathParameters,
            description  : operation.description,
            summary      : operation.summary,
            operationId  : operation.operationId,
            mimeType     : operation.mimeType,
            controller   : operation.controller,
            action       : operation.method,
            body         : operation.body,
            responseType : operation.responseType,
            filters      : listFilters(operation)
        ] as Map<String, Object>
    }

    private static Map<String, Integer> operationFamilies(List<McpHttpResourceRegistry.McpHttpOperation> operations) {
        Map<String, Integer> families = new LinkedHashMap<String, Integer>()
        for (McpHttpResourceRegistry.McpHttpOperation operation : operations) {
            String key = operation.operationKind ?: 'unknown'
            families.put(key, Integer.valueOf((families.get(key) ?: 0) + 1))
        }
        families
    }

    private static List<String> resourceTypes(List<McpHttpResourceRegistry.McpHttpOperation> operations) {
        operations.collect {McpHttpResourceRegistry.McpHttpOperation operation -> operation.resourceType}
            .findAll {String value -> value != null && !value.trim().isEmpty()}
            .unique()
            .sort() as List<String>
    }

    private static List<String> listFilters(McpHttpResourceRegistry.McpHttpOperation operation) {
        if (operation.operationKind != 'list') {
            return Collections.<String>emptyList()
        }
        ['offset', 'max', 'sort', 'order', 'label', 'description', 'code', 'definition', 'domainType', 'all'] as List<String>
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
