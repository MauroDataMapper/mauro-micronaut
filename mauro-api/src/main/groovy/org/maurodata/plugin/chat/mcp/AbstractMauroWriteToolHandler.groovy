package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.http.HttpHeaders
import io.micronaut.runtime.server.EmbeddedServer

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
abstract class AbstractMauroWriteToolHandler extends AbstractAnnotatedToolHandler {

    protected final McpHttpResourceRegistry resourceRegistry
    protected final EmbeddedServer embeddedServer
    protected final HttpClient httpClient
    protected final String expectedOperation
    protected final String expectedHttpMethod

    protected AbstractMauroWriteToolHandler(Class<?> metadataSource,
                                           McpHttpResourceRegistry resourceRegistry,
                                           EmbeddedServer embeddedServer,
                                           String expectedOperation,
                                           String expectedHttpMethod) {
        super(metadataSource)
        this.resourceRegistry = resourceRegistry
        this.embeddedServer = embeddedServer
        this.expectedOperation = expectedOperation
        this.expectedHttpMethod = expectedHttpMethod
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments ?: [:] as Map<String, Object>
        McpHttpResourceRegistry.McpHttpOperation operation = resolveOperation(safeArguments)
        if (operation == null) {
            List<McpHttpResourceRegistry.McpHttpOperation> candidates = resourceRegistry.listOperations(asString(safeArguments.get('resourceType')), expectedOperation)
            return [
                executed: false,
                needsOperationSelection: true,
                expectedOperation: expectedOperation,
                candidates: candidates.take(20).collect {McpHttpResourceRegistry.McpHttpOperation candidate -> operationSummary(candidate)}
            ] as Map<String, Object>
        }
        if (operation.operationKind != expectedOperation || operation.httpMethod != expectedHttpMethod) {
            return [
                executed: false,
                rejected: true,
                reason: "Selected route is ${operation.operationKind}/${operation.httpMethod}, not ${expectedOperation}/${expectedHttpMethod}.",
                selectedOperation: operationSummary(operation)
            ] as Map<String, Object>
        }
        String path = resolvedPath(operation, safeArguments)
        List<String> missing = missingPathParameters(operation, path)
        if (!missing.isEmpty()) {
            return [
                executed: false,
                missingPathParameters: missing,
                selectedOperation: operationSummary(operation)
            ] as Map<String, Object>
        }
        if (!asBoolean(safeArguments.get('confirmed'), false)) {
            return [
                executed: false,
                needsConfirmation: true,
                confirmationRequired: true,
                selectedOperation: operationSummary(operation),
                requestPath: path,
                body: safeArguments.get('body'),
                exactToolCallAfterConfirmation: confirmationCall(operation, path, safeArguments)
            ] as Map<String, Object>
        }

        HttpResponse<String> response = send(operation.httpMethod, path, safeArguments.get('body'), forwardedHeaders(safeArguments.get('_mauroForwardHeaders')))
        String content = response.body() ?: ''
        Object parsed = parseJson(content)
        Map<String, Object> out = [
            executed: true,
            operation: operationSummary(operation),
            requestPath: path,
            statusCode: response.statusCode(),
            mimeType: response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(operation.mimeType ?: 'application/json'),
            content: content
        ] as Map<String, Object>
        if (parsed != null) {
            out.put('data', parsed)
        }
        out
    }

    @Override
    String modelText(Map<String, Object> result) {
        if (Boolean.TRUE.equals(result.get('needsOperationSelection'))) {
            List<String> candidates = new ArrayList<String>()
            for (Object candidateObj : result.get('candidates') instanceof Collection ? (Collection<?>) result.get('candidates') : []) {
                if (candidateObj instanceof Map) {
                    Map<?, ?> candidate = (Map<?, ?>) candidateObj
                    candidates.add("${candidate.get('resourceType')} | ${candidate.get('operation')} | ${candidate.get('method')} ${candidate.get('path')} | ${candidate.get('name')}".toString())
                }
            }
            return renderModelTextSections([
                'Tool Call Status': ["Tool ${name()} needs one ${expectedOperation} operation before it can execute.".toString()],
                'Returned Data': candidates ?: ['No matching operation was found.'],
                'Answer Instructions': [
                    'Use mauro_describe to identify the intended operation if the route is unclear.',
                    'Use mauro_schema to inspect the body and parameters before confirmation.'
                ]
            ] as Map<String, Object>)
        }
        if (Boolean.TRUE.equals(result.get('rejected'))) {
            return renderModelTextSections([
                'Tool Call Status': ["Tool ${name()} rejected the selected operation.".toString()],
                'Result Metadata': [String.valueOf(result.get('reason'))],
                'Answer Instructions': ['Choose an operation whose operation kind and HTTP method match this tool.']
            ] as Map<String, Object>)
        }
        if (result.get('missingPathParameters') instanceof Collection && !((Collection<?>) result.get('missingPathParameters')).isEmpty()) {
            return renderModelTextSections([
                'Tool Call Status': ["Tool ${name()} cannot execute until path parameters are supplied.".toString()],
                'Result Metadata': ["Missing path parameters: ${result.get('missingPathParameters')}"],
                'Answer Instructions': ['Ask a focused question for the missing identifier values, or use known ids from prior tool results.']
            ] as Map<String, Object>)
        }
        if (Boolean.TRUE.equals(result.get('needsConfirmation'))) {
            return renderModelTextSections([
                'Tool Call Status': ["Tool ${name()} did not execute because confirmation is required.".toString()],
                'Result Metadata': [
                    "Request path: ${result.get('requestPath')}",
                    "Confirmation required: ${result.get('confirmationRequired')}"
                ],
                'Exact Tool Call After Confirmation': [JsonOutput.toJson(result.get('exactToolCallAfterConfirmation'))],
                'Answer Instructions': [
                    'Explain the action that would be taken and ask for explicit confirmation before executing it.',
                    'Do not execute the write or delete operation until confirmed is true.'
                ]
            ] as Map<String, Object>)
        }

        int status = asInteger(result.get('statusCode'), 0)
        boolean success = status >= 200 && status < 300
        String content = asString(result.get('content')) ?: ''
        return renderModelTextSections([
            'Tool Call Status': ["Tool ${name()} completed with HTTP ${status}.".toString()],
            'Result Metadata': [
                "Request path: ${result.get('requestPath')}",
                "MIME type: ${result.get('mimeType')}",
                "Content length: ${content.length()}"
            ],
            'Returned Data': [
                content.length() > 12000 ? content.substring(0, 12000) + '\n[truncated for model context]' : content
            ],
            'Answer Instructions': success ? [
                'Summarise the successful change and the returned resource fields.',
                'Mention the HTTP status if useful for auditability.'
            ] : [
                "Explain that the operation failed and include HTTP status ${status}.".toString(),
                'Do not describe the returned body as a successful change.'
            ]
        ] as Map<String, Object>)
    }

    private McpHttpResourceRegistry.McpHttpOperation resolveOperation(Map<String, Object> arguments) {
        String operationUri = asString(arguments.get('operationUri'))
        if (operationUri != null && !operationUri.trim().isEmpty()) {
            return resourceRegistry.findOperationByUri(operationUri)
        }
        String path = asString(arguments.get('path'))
        String method = asString(arguments.get('method')) ?: expectedHttpMethod
        if (path != null && !path.trim().isEmpty()) {
            return resourceRegistry.findOperationByPathAndMethod(path, method)
        }
        List<McpHttpResourceRegistry.McpHttpOperation> candidates = resourceRegistry.listOperations(asString(arguments.get('resourceType')), expectedOperation)
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

    private static String resolvedPath(McpHttpResourceRegistry.McpHttpOperation operation, Map<String, Object> arguments) {
        String path = asString(arguments.get('path')) ?: operation.path
        Object pathParametersObj = arguments.get('pathParameters')
        if (pathParametersObj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) pathParametersObj).entrySet()) {
                if (entry.key != null && entry.value != null) {
                    path = path.replace('{' + String.valueOf(entry.key) + '}', encodePathSegment(String.valueOf(entry.value)))
                }
            }
        }
        path
    }

    private static List<String> missingPathParameters(McpHttpResourceRegistry.McpHttpOperation operation, String path) {
        List<String> missing = new ArrayList<String>()
        for (String parameter : operation.pathParameters ?: []) {
            if (path.contains('{' + parameter + '}')) {
                missing.add(parameter)
            }
        }
        missing
    }

    private Map<String, Object> confirmationCall(McpHttpResourceRegistry.McpHttpOperation operation, String path, Map<String, Object> arguments) {
        [
            name: name(),
            arguments: [
                operationUri: operation.operationUri,
                path: path,
                body: arguments.get('body'),
                confirmed: true
            ] as Map<String, Object>
        ] as Map<String, Object>
    }

    private HttpResponse<String> send(String method, String path, Object body, Map<String, List<String>> headers) {
        URI target = embeddedServer.URI.resolve(path.startsWith('/') ? path : '/' + path)
        String jsonBody = body == null ? '' : (body instanceof String ? String.valueOf(body) : JsonOutput.toJson(body))
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(target)
            .timeout(Duration.ofSeconds(30))
            .header(HttpHeaders.ACCEPT, 'application/json')
        if (method != 'DELETE' || jsonBody) {
            builder.header(HttpHeaders.CONTENT_TYPE, 'application/json')
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.value ?: []) {
                if (value != null && !value.trim().isEmpty()) {
                    builder.header(entry.key, value)
                }
            }
        }
        if (method == 'POST') {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        } else if (method == 'PUT') {
            builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
        } else if (method == 'DELETE') {
            builder.method('DELETE', jsonBody ? HttpRequest.BodyPublishers.ofString(jsonBody) : HttpRequest.BodyPublishers.noBody())
        } else {
            throw new IllegalArgumentException("Unsupported write HTTP method: ${method}")
        }
        httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private static Map<String, List<String>> forwardedHeaders(Object raw) {
        if (!(raw instanceof Map)) {
            return Collections.<String, List<String>>emptyMap()
        }
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>()
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.key == null || !(entry.value instanceof Collection)) {
                continue
            }
            List<String> values = new ArrayList<String>()
            for (Object value : (Collection<?>) entry.value) {
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    values.add(String.valueOf(value))
                }
            }
            if (!values.isEmpty()) {
                headers.put(String.valueOf(entry.key), values)
            }
        }
        headers
    }

    private static Object parseJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null
        }
        try {
            return new JsonSlurper().parseText(text)
        } catch (Throwable ignored) {
            return null
        }
    }

    private static String encodePathSegment(String value) {
        URLEncoder.encode(value, 'UTF-8')
    }

    protected static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    protected static boolean asBoolean(Object value, boolean fallback) {
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }

    protected static int asInteger(Object value, int fallback) {
        if (value == null) {
            return fallback
        }
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        text.trim().isEmpty() ? fallback : Integer.valueOf(text)
    }
}
