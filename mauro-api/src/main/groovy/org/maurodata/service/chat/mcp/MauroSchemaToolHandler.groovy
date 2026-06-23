package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_schema',
    description = 'Describe the route parameters, request body type, response type, and best-available schema hints for a Mauro API operation.',
    purpose = 'Inspect the expected shape of an operation before calling mauro_create, mauro_update, or mauro_delete.',
    useWhen = [
        'checking request body shape before creating or updating a Mauro resource',
        'checking path parameters and query parameters for a selected Mauro API operation',
        'understanding what fields are required by the route signature before invoking a write tool'
    ],
    avoidWhen = [
        'searching catalogue content by keyword; use mauro_keyword_search',
        'reading a known resource; use mauro_get',
        'listing data; use mauro_list'
    ],
    examples = [
        'schema for DataModel create',
        'show schema for operation URI mauro-api://operation/post/api/folders/{folderId}/dataModels',
        'what body does update Terminology expect?'
    ],
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Mauro resource type, for example DataModel or Terminology."},"operation":{"type":"string","enum":["list","get","create","update","delete","other"],"description":"Operation family to inspect."},"operationUri":{"type":"string","description":"Operation URI returned by mauro_describe, for example mauro-api://operation/post/api/folders/{folderId}/dataModels."},"path":{"type":"string","description":"Route path to inspect, for example /api/dataModels/{id}."},"method":{"type":"string","enum":["GET","POST","PUT","DELETE"],"description":"HTTP method for the path. Required when path is supplied and multiple methods exist."}}}'
)
class MauroSchemaToolHandler extends AbstractAnnotatedToolHandler {

    private final McpHttpResourceRegistry resourceRegistry

    MauroSchemaToolHandler(McpHttpResourceRegistry resourceRegistry) {
        super(MauroSchemaToolHandler)
        this.resourceRegistry = resourceRegistry
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        McpHttpResourceRegistry.McpHttpOperation operation = resolveOperation(arguments ?: [:] as Map<String, Object>)
        if (operation == null) {
            List<McpHttpResourceRegistry.McpHttpOperation> candidates = resourceRegistry.listOperations(asString(arguments.get('resourceType')), asString(arguments.get('operation')))
            return [
                found: false,
                count: candidates.size(),
                candidates: candidates.take(20).collect {McpHttpResourceRegistry.McpHttpOperation candidate -> operationSummary(candidate)}
            ] as Map<String, Object>
        }
        [
            found: true,
            exactJsonSchemaAvailable: false,
            exactness: 'route-signature',
            operation: operationSummary(operation),
            pathParameters: operation.pathParameters,
            parameters: operation.parameters,
            body: operation.body,
            responseType: operation.responseType,
            consumes: operation.consumes,
            produces: operation.produces,
            notes: [
                'This schema is derived from Micronaut route signatures and OpenAPI operation metadata.',
                'It is suitable for choosing arguments and body type, but it is not an exhaustive domain validation schema.',
                'Use mauro_describe to find alternative operations if this operation is not the intended route.'
            ]
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        if (!Boolean.TRUE.equals(result.get('found'))) {
            List<String> candidates = new ArrayList<String>()
            for (Object candidateObj : result.get('candidates') instanceof Collection ? (Collection<?>) result.get('candidates') : []) {
                if (candidateObj instanceof Map) {
                    Map<?, ?> candidate = (Map<?, ?>) candidateObj
                    candidates.add("${candidate.get('resourceType')} | ${candidate.get('operation')} | ${candidate.get('method')} ${candidate.get('path')} | ${candidate.get('name')}".toString())
                }
            }
            return renderModelTextSections([
                'Tool Call Status': ['Tool mauro_schema could not identify a single operation.'],
                'Result Metadata': ["Candidate operations: ${result.get('count')}"],
                'Returned Data': candidates ?: ['No matching operation was found.'],
                'Answer Instructions': [
                    'If several candidates are returned, choose the one matching the requested resource type and operation.',
                    'If no candidate matches, call mauro_describe to inspect available operations.'
                ]
            ] as Map<String, Object>)
        }

        Map<?, ?> operation = result.get('operation') instanceof Map ? (Map<?, ?>) result.get('operation') : [:]
        renderModelTextSections([
            'Tool Call Status': ['Tool mauro_schema succeeded.'],
            'Result Metadata': [
                "Resource type: ${operation.get('resourceType')}",
                "Operation: ${operation.get('operation')}",
                "Route: ${operation.get('method')} ${operation.get('path')}",
                "Schema exactness: ${result.get('exactness')}",
                "Exact JSON schema available: ${result.get('exactJsonSchemaAvailable')}"
            ],
            'Route Parameters': formatList(result.get('pathParameters')),
            'Request Parameters': formatParameterMaps(result.get('parameters')),
            'Request Body': formatMap(result.get('body')),
            'Response': ["Response type: ${result.get('responseType') ?: 'not reported'}"],
            'Notes': result.get('notes'),
            'Answer Instructions': [
                'Use this schema to choose path parameters, query parameters, and request body type.',
                'For create and update operations, prepare a JSON body matching the reported body type before calling the write tool.',
                'Do not treat this route-signature schema as exhaustive field-level validation.'
            ]
        ] as Map<String, Object>)
    }

    private McpHttpResourceRegistry.McpHttpOperation resolveOperation(Map<String, Object> arguments) {
        String operationUri = asString(arguments.get('operationUri'))
        if (operationUri != null && !operationUri.trim().isEmpty()) {
            return resourceRegistry.findOperationByUri(operationUri)
        }
        String path = asString(arguments.get('path'))
        String method = asString(arguments.get('method'))
        if (path != null && method != null) {
            return resourceRegistry.findOperationByPathAndMethod(path, method)
        }
        List<McpHttpResourceRegistry.McpHttpOperation> candidates = resourceRegistry.listOperations(asString(arguments.get('resourceType')), asString(arguments.get('operation')))
        candidates.size() == 1 ? candidates.first() : null
    }

    private static Map<String, Object> operationSummary(McpHttpResourceRegistry.McpHttpOperation operation) {
        [
            resourceType: operation.resourceType,
            operation: operation.operationKind,
            operationUri: operation.operationUri,
            method: operation.httpMethod,
            path: operation.path,
            name: operation.name,
            operationId: operation.operationId,
            summary: operation.summary,
            description: operation.description,
            body: operation.body,
            responseType: operation.responseType
        ] as Map<String, Object>
    }

    private static List<String> formatList(Object value) {
        if (!(value instanceof Collection) || ((Collection<?>) value).isEmpty()) {
            return Collections.<String>emptyList()
        }
        ((Collection<?>) value).collect {Object item -> String.valueOf(item)} as List<String>
    }

    private static List<String> formatParameterMaps(Object value) {
        if (!(value instanceof Collection)) {
            return Collections.<String>emptyList()
        }
        List<String> lines = new ArrayList<String>()
        for (Object item : (Collection<?>) value) {
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item
                lines.add("${map.get('name')} | type: ${map.get('type')} | body: ${map.get('body')} | query: ${map.get('query')} | nullable: ${map.get('nullable')}".toString())
            }
        }
        lines
    }

    private static List<String> formatMap(Object value) {
        if (!(value instanceof Map)) {
            return ['No request body reported for this operation.'] as List<String>
        }
        Map<?, ?> map = (Map<?, ?>) value
        ["name: ${map.get('name')}; type: ${map.get('type')}; required: ${map.get('required')}".toString()] as List<String>
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}
