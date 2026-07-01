package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.http.HttpHeaders
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_get',
    description = 'Read a known read-only Mauro API resource URI.',
    purpose = 'Fetch structured data from a known read-only Mauro API resource URI, preserving the caller HTTP authorisation context.',
    useWhen = [
        'reading a known mauro-api://http-get resource URI',
        'inspecting a known catalogue object when its resource URI or expanded template is available',
        'retrieving structured Mauro API data after mauro_describe identifies the right resource',
        'reading a known DataModel id using mauro-api://http-get/api/dataModels/{id} after replacing {id}'
    ],
    avoidWhen = [
        'discovering unknown catalogue items by search; use mauro_search first',
        'looking up available resource routes; use mauro_describe first'
    ],
    examples = [
        'read mauro-api://http-get/api/dataModels/019ddee8-a68d-7fc9-b84b-017d9e687d36',
        'read the DataModel.show resource for this known id',
        'after mauro_search returns a DataModel id, call mauro_get with mauro-api://http-get/api/dataModels/ID'
    ],
    inputSchema = '{"type":"object","properties":{"uri":{"type":"string","description":"Concrete resource URI to read, for example mauro-api://http-get/api/dataModels/ID. URI templates must be expanded before reading."}},"required":["uri"]}'
)
class ResourceReadToolHandler extends AbstractAnnotatedToolHandler {

    private final McpHttpResourceRegistry resourceRegistry
    private final EmbeddedServer embeddedServer
    private final HttpClient httpClient

    ResourceReadToolHandler(McpHttpResourceRegistry resourceRegistry, EmbeddedServer embeddedServer) {
        super(ResourceReadToolHandler)
        this.resourceRegistry = resourceRegistry
        this.embeddedServer = embeddedServer
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String uri = asString(arguments.get('uri'))
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException('mauro_get requires uri')
        }
        McpHttpResourceRegistry.McpHttpResource resource = resourceRegistry.findByUri(uri)
        if (resource == null) {
            throw new IllegalArgumentException("Unknown resource URI: ${uri}")
        }
        String path = pathFromResourceUri(uri)
        HttpResponse<String> response = readHttpGet(path, forwardedHeaders(arguments.get('_mauroForwardHeaders')))
        String body = response.body() ?: ''
        Map<String, Object> parsedObject = parseJsonObject(body)
        Map<String, Object> out = [
            uri        : uri,
            path       : path,
            name       : resource.name,
            resourceDescription: resource.description,
            statusCode : response.statusCode(),
            mimeType   : response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(resource.mimeType ?: 'application/json'),
            content    : body
        ] as Map<String, Object>
        if (!parsedObject.isEmpty()) {
            out.put('data', parsedObject)
        }
        putIfPresent(out, 'id', parsedObject.get('id'))
        putIfPresent(out, 'label', parsedObject.get('label'))
        putIfPresent(out, 'domainType', parsedObject.get('domainType'))
        putIfPresent(out, 'description', parsedObject.get('description'))
        out
    }

    @Override
    String modelText(Map<String, Object> result) {
        int status = asInteger(result.get('statusCode'), 0)
        String content = asString(result.get('content')) ?: ''
        boolean success = status >= 200 && status < 300
        renderModelTextSections([
            'Tool Call Status'   : ["Tool mauro_get completed with HTTP ${status}.".toString()],
            'Current Answer Source': currentAnswerSource(status, success, result),
            'Result Metadata'    : [
                "URI: ${result.get('uri')}",
                "Path: ${result.get('path')}",
                "Resource: ${result.get('name')}",
                "Resource route: ${result.get('resourceDescription') ?: ''}",
                "MIME type: ${result.get('mimeType')}",
                "Content length: ${content.length()}"
            ],
            'Returned Data'      : [
                content.length() > 12000 ? content.substring(0, 12000) + '\n[truncated for model context]' : content
            ],
            'Interpretation'     : resourceInterpretation(status, success),
            'Answer Instructions': resourceAnswerInstructions(status, success),
            'Recovery Gate'      : recoveryGate(status, success),
            'Identity Check branch': identityCheckBranch(status, success),
            'Final Failure branch': finalFailureBranch(status, success)
        ] as Map<String, Object>)
    }

    private static List<String> currentAnswerSource(int status, boolean success, Map<String, Object> result) {
        if (!success) {
            return Collections.<String>emptyList()
        }
        List<String> source = [
            'This latest mauro_get result is the direct answer source for the current user request.',
            'Answer from this fetched resource, not from an earlier mauro_search result page.',
            'Use earlier search/list results only as provenance for how this URI/id was selected.'
        ] as List<String>
        String label = asString(result.get('label'))
        String id = asString(result.get('id'))
        String domainType = asString(result.get('domainType'))
        if (label != null || id != null || domainType != null) {
            source.add("Fetched item: ${label ?: '(no label)'}; domainType: ${domainType ?: '(not reported)'}; id: ${id ?: '(not reported)'}.".toString())
        }
        source
    }

    private static List<String> resourceInterpretation(int status, boolean success) {
        if (!success) {
            return [
                "The backend HTTP status is ${status}, so this resource read did not succeed.".toString()
            ] as List<String>
        }
        Collections.<String>emptyList()
    }

    private static List<String> resourceAnswerInstructions(int status, boolean success) {
        if (!success) {
            return [
                "COMMON: The mauro_get call returned HTTP ${status} and did not return successful resource content.".toString(),
                'COMMON: Do not interpret the returned body as successful resource content.',
                'COMMON: Choose exactly one recovery branch when a Recovery Gate is present: IC or FF.'
            ] as List<String>
        }
        [
            'Use the returned resource content as authoritative structured Mauro API data for this turn.',
            'The current user asked to inspect/read this specific resource; summarise this resource rather than repeating earlier search results.',
            'When answering, summarise the relevant fields and any available interpretations rather than dumping large JSON unless the user asks for raw JSON.'
        ] as List<String>
    }

    private static List<String> recoveryGate(int status, boolean success) {
        if (success || status != 404) {
            return Collections.<String>emptyList()
        }
        [
            'Choose exactly one branch flag for this failed read: IC or FF.',
            'IC = Identity Check. Choose IC when the current user request contains, names, quotes, or refers to a catalogue label, form name, model name, or previous result.',
            'FF = Final Failure. Choose FF only after IC has been attempted and the verified corrected lookup still fails, or when the user request contains no label/name/reference that can be searched.',
            'The current user request itself is enough label context for IC; do not ask the user to repeat a label that is already present in the conversation.',
            'If unsure, choose IC and emit a structured read-only tool call before giving a final failure answer.',
            'IC is a tool-call branch, not a prose/status-update branch.'
        ] as List<String>
    }

    private static List<String> identityCheckBranch(int status, boolean success) {
        if (success || status != 404) {
            return Collections.<String>emptyList()
        }
        [
            'IC: Treat this HTTP 404 as a possible wrong-ID recovery case, not as the final answer yet.',
            'IC: Identify the catalogue label, form name, model name, or previous-result reference from the current user request or recent conversation.',
            'IC: If prior mauro_search or mauro_list Returned Data contains that exact label, compare that row ID with the ID used in this failed URI.',
            'IC: If the failed URI uses an ID from a different label, call mauro_get again with the ID from the row whose label exactly matches the user request.',
            'IC: If the matching label is not visible in prior Returned Data, call mauro_search for the exact requested label with the appropriate domainTypes; for a form/model label use domainTypes ["DataModel"].',
            'IC: After mauro_search returns the exact matching label, call mauro_get with that row ID.',
            'IC: Emit the mauro_search or mauro_get tool call now. Do not answer with "I will search", "please wait", or other prose promising a later tool call.',
            'IC: This lookup is work you should perform for the user; do not ask the user to repeat the label before searching when the label is already in the conversation.',
            'IC: Do not give a final not-found answer while this label-to-ID check or retry is still available.'
        ] as List<String>
    }

    private static List<String> finalFailureBranch(int status, boolean success) {
        if (success) {
            return Collections.<String>emptyList()
        }
        if (status == 404) {
            return [
                "FF: Explain that the resource could not be read and include HTTP status ${status}.".toString(),
                'FF: Use this branch only after actively attempting IC when any requested label/name/reference is present in the conversation.',
                'FF: Do not claim the catalogue item itself does not exist unless the verified item lookup also failed.'
            ] as List<String>
        }
        [
            "FF: Explain that the resource could not be read and include HTTP status ${status}.".toString()
        ] as List<String>
    }

    private HttpResponse<String> readHttpGet(String path, Map<String, List<String>> headers) {
        URI target = embeddedServer.URI.resolve(path.startsWith('/') ? path : '/' + path)
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(target)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .header(HttpHeaders.ACCEPT, 'application/json')
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.value ?: []) {
                if (value != null && !value.trim().isEmpty()) {
                    builder.header(entry.key, value)
                }
            }
        }
        httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private static String pathFromResourceUri(String uri) {
        if (uri == null || !uri.startsWith(McpHttpResourceRegistry.URI_PREFIX)) {
            throw new IllegalArgumentException("Unsupported resource URI: ${uri}")
        }
        uri.substring(McpHttpResourceRegistry.URI_PREFIX.length())
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

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static Map<String, Object> parseJsonObject(String text) {
        if (text == null || text.trim().isEmpty()) {
            return [:] as Map<String, Object>
        }
        try {
            Object parsed = new JsonSlurper().parseText(text)
            if (parsed instanceof Map) {
                @SuppressWarnings('unchecked')
                Map<String, Object> map = (Map<String, Object>) parsed
                return map
            }
        } catch (Throwable ignored) {
            // Resource content may not be JSON. Keep the raw content authoritative.
        }
        [:] as Map<String, Object>
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            target.put(key, value)
        }
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
}
