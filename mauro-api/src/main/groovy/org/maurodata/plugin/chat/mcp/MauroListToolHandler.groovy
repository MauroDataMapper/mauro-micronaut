package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.http.HttpHeaders
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_list',
    description = 'List Mauro API resources using concrete read-only ListResponse-style GET routes with pagination and filters.',
    purpose = 'Dispatch to Mauro API GET list endpoints that return ListResponse-style data. Use this for typed resource listing when the resource route is known or can be selected from a resource type.',
    useWhen = [
        'listing resources of a known Mauro API type such as DataModel, Terminology, CodeSet, DataClass, DataElement, Folder, ClassificationScheme, or Classifier',
        'using pagination, sorting, or simple ListResponse filters over a concrete Mauro API list route',
        'reading a known concrete mauro-api://http-get list URI with optional PaginationParams-style query parameters'
    ],
    avoidWhen = [
        'searching catalogue content; use mauro_search',
        'reading a single known resource by URI or id; use mauro_get',
        'discovering available resource templates and operations without listing data; use mauro_describe'
    ],
    examples = [
        'list DataModels => resourceType "DataModel", max 10, offset 0',
        'list Terminologies with label diabetes => resourceType "Terminology", label "diabetes"',
        'list from known route => uri "mauro-api://http-get/api/dataModels", max 20'
    ],
    filtering = [
        'supports PaginationParams-style filters where the selected endpoint supports them: offset, max, sort, order, label, description, code, definition, all, and domainType',
        'resourceType chooses a list route by Mauro API type; if multiple concrete list routes match, choose from the returned route choices or use mauro_describe'
    ],
    paging = [
        'use max as page size and offset for subsequent pages',
        'preserve the selected uri/path/resourceType and filters when requesting another page'
    ],
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Mauro API resource type to list, for example DataModel, Terminology, CodeSet, DataClass, DataElement, Folder, ClassificationScheme, or Classifier."},"resourceName":{"type":"string","description":"Exact or partial MCP resource route name, for example DataModel.listAll."},"uri":{"type":"string","description":"Concrete mauro-api://http-get list resource URI. URI templates must be expanded before listing."},"path":{"type":"string","description":"Concrete Mauro API GET list path, for example /api/dataModels."},"max":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum items to return for this page. Omit for 10."},"offset":{"type":"integer","minimum":0,"description":"Zero-based offset for pagination. Omit for 0."},"sort":{"type":"string","description":"Optional sort field for endpoints using PaginationParams."},"order":{"type":"string","enum":["asc","desc"],"description":"Optional sort order. Omit for asc."},"label":{"type":"string","description":"Optional label filter for endpoints using PaginationParams."},"description":{"type":"string","description":"Optional description filter for endpoints using PaginationParams."},"code":{"type":"string","description":"Optional code filter for term-like endpoints using PaginationParams."},"definition":{"type":"string","description":"Optional definition filter for term-like endpoints using PaginationParams."},"domainType":{"type":"string","description":"Optional domainType/dataTypeKind filter for endpoints using PaginationParams."},"all":{"type":"boolean","description":"When true, request all items if the endpoint supports PaginationParams all=true. Use sparingly."},"filters":{"type":"object","additionalProperties":true,"description":"Additional query parameters for the selected GET route."}}}'
)
class MauroListToolHandler extends AbstractAnnotatedToolHandler {

    private static final int DEFAULT_PAGE_SIZE = 10
    private static final int MAX_PAGE_SIZE = 100

    private final McpHttpResourceRegistry resourceRegistry
    private final EmbeddedServer embeddedServer
    private final HttpClient httpClient

    MauroListToolHandler(McpHttpResourceRegistry resourceRegistry, EmbeddedServer embeddedServer) {
        super(MauroListToolHandler)
        this.resourceRegistry = resourceRegistry
        this.embeddedServer = embeddedServer
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        List<McpHttpResourceRegistry.McpHttpResource> candidates = matchingListResources(arguments ?: [:] as Map<String, Object>)
        int max = Math.max(1, Math.min(asInteger(arguments.get('max'), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE))
        int offset = Math.max(0, asInteger(arguments.get('offset'), 0))

        if (candidates.size() != 1) {
            return [
                resourceType: asString(arguments.get('resourceType')),
                resourceName: asString(arguments.get('resourceName')),
                uri: asString(arguments.get('uri')),
                path: asString(arguments.get('path')),
                count: candidates.size(),
                resources: candidates.take(20).collect {McpHttpResourceRegistry.McpHttpResource resource -> resourceToMap(resource)},
                max: max,
                offset: offset,
                needsResourceSelection: true
            ] as Map<String, Object>
        }

        McpHttpResourceRegistry.McpHttpResource resource = candidates.first()
        String path = pathFromResource(resource, arguments)
        String queryPath = appendQuery(path, queryParameters(arguments, max, offset))
        HttpResponse<String> response = readHttpGet(queryPath, forwardedHeaders(arguments.get('_mauroForwardHeaders')))
        String body = response.body() ?: ''
        Object parsed = parseJson(body)
        Map<String, Object> out = [
            uri: resource.resourceUri,
            path: resource.path,
            requestPath: queryPath,
            name: resource.name,
            resourceDescription: resource.description,
            statusCode: response.statusCode(),
            mimeType: response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(resource.mimeType ?: 'application/json'),
            content: body,
            max: max,
            offset: offset,
            resourceType: asString(arguments.get('resourceType'))
        ] as Map<String, Object>
        if (parsed instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> map = (Map<String, Object>) parsed
            out.put('data', map)
            putIfPresent(out, 'count', map.get('count'))
            if (map.get('items') instanceof Collection) {
                out.put('items', map.get('items'))
                out.put('returned', Integer.valueOf(((Collection<?>) map.get('items')).size()))
            }
        }
        out
    }

    @Override
    String modelText(Map<String, Object> result) {
        boolean selectionNeeded = Boolean.TRUE.equals(result.get('needsResourceSelection'))
        if (selectionNeeded) {
            List<String> resources = new ArrayList<String>()
            for (Object resourceObj : result.get('resources') instanceof Collection ? (Collection<?>) result.get('resources') : []) {
                if (resourceObj instanceof Map) {
                    Map<?, ?> resource = (Map<?, ?>) resourceObj
                    resources.add("${resource.get('name')} | ${resource.get('uri')} | ${resource.get('description')}".toString())
                }
            }
            return renderModelTextSections([
                'Tool Call Status': ['Tool mauro_list needs one concrete list resource before it can list data.'],
                'Result Metadata': [
                    "Resource type: ${result.get('resourceType') ?: ''}",
                    "Resource name: ${result.get('resourceName') ?: ''}",
                    "Matched list resources: ${result.get('count')}"
                ],
                'Returned Data': resources ?: ['No matching concrete list resources were found.'],
                'Answer Instructions': [
                    'If exactly one returned route matches the user request, call mauro_list again with that exact uri.',
                    'If several routes could match, ask a focused question for the missing resource type, parent id, or route choice.',
                    'If no list route matches, use mauro_describe to inspect available resource routes.'
                ]
            ] as Map<String, Object>)
        }

        int status = asInteger(result.get('statusCode'), 0)
        boolean success = status >= 200 && status < 300
        List<?> items = result.get('items') instanceof List ? (List<?>) result.get('items') : Collections.emptyList()
        Integer count = asIntegerOrNull(result.get('count'))
        int max = asInteger(result.get('max'), DEFAULT_PAGE_SIZE)
        int offset = asInteger(result.get('offset'), 0)
        boolean hasMore = count != null && offset + items.size() < count
        String content = asString(result.get('content')) ?: ''

        List<String> returnedData = new ArrayList<String>()
        if (!items.isEmpty()) {
            int index = 1
            for (Object item : items.take(max)) {
                returnedData.add("${index}. ${summariseItem(item)}".toString())
                index++
            }
        } else {
            returnedData.add(content.length() > 12000 ? content.substring(0, 12000) + '\n[truncated for model context]' : content)
        }

        renderModelTextSections([
            'Tool Call Status': ["Tool mauro_list completed with HTTP ${status}.".toString()],
            'Result Metadata': [
                "Resource: ${result.get('name')}",
                "URI: ${result.get('uri')}",
                "Request path: ${result.get('requestPath')}",
                "Total count: ${count == null ? 'not reported' : count}",
                "Returned items: ${items.size()}",
                "Offset: ${offset}",
                "Max: ${max}",
                "Has more results: ${hasMore}"
            ],
            'Returned Data': returnedData,
            'Answer Instructions': success ? [
                'Present the listed items clearly and include the total count when reported.',
                'If more results are available, mention that you can fetch the next page with the same selected resource and filters using the next offset.',
                'Use mauro_get for a single returned item when the user asks to inspect it more closely.'
            ] : [
                "Explain that the list resource could not be read and include HTTP status ${status}.".toString(),
                'Do not interpret the returned body as a successful list response.'
            ]
        ] as Map<String, Object>)
    }

    private List<McpHttpResourceRegistry.McpHttpResource> matchingListResources(Map<String, Object> arguments) {
        String uri = asString(arguments.get('uri'))
        if (uri != null && !uri.trim().isEmpty()) {
            McpHttpResourceRegistry.McpHttpResource resource = resourceRegistry.findByUri(uri)
            return resource != null && isListResource(resource) && !resource.template ? [resource] : Collections.<McpHttpResourceRegistry.McpHttpResource>emptyList()
        }
        String path = asString(arguments.get('path'))
        if (path != null && !path.trim().isEmpty()) {
            McpHttpResourceRegistry.McpHttpResource resource = resourceRegistry.findByUri(McpHttpResourceRegistry.URI_PREFIX + pathWithoutQuery(path))
            return resource != null && isListResource(resource) && !resource.template ? [resource] : Collections.<McpHttpResourceRegistry.McpHttpResource>emptyList()
        }

        List<McpHttpResourceRegistry.McpHttpResource> resources = resourceRegistry.listConcreteResources()
            .findAll {McpHttpResourceRegistry.McpHttpResource resource -> isListResource(resource)}

        String resourceName = asString(arguments.get('resourceName'))
        if (resourceName != null && !resourceName.trim().isEmpty()) {
            String lower = resourceName.toLowerCase(Locale.ROOT)
            resources = resources.findAll {McpHttpResourceRegistry.McpHttpResource resource ->
                resource.name?.toLowerCase(Locale.ROOT) == lower ||
                    resource.name?.toLowerCase(Locale.ROOT)?.contains(lower) ||
                    resource.path?.toLowerCase(Locale.ROOT)?.contains(lower)
            } as List<McpHttpResourceRegistry.McpHttpResource>
        }

        String resourceType = asString(arguments.get('resourceType'))
        if (resourceType != null && !resourceType.trim().isEmpty()) {
            String lower = resourceType.toLowerCase(Locale.ROOT)
            resources = resources.findAll {McpHttpResourceRegistry.McpHttpResource resource ->
                resource.name?.toLowerCase(Locale.ROOT)?.startsWith(lower + '.') ||
                    resource.controller?.toLowerCase(Locale.ROOT)?.endsWith('.' + lower + 'controller') ||
                    resource.path?.toLowerCase(Locale.ROOT)?.contains('/' + lower + 's')
            } as List<McpHttpResourceRegistry.McpHttpResource>
        }
        resources
    }

    private static boolean isListResource(McpHttpResourceRegistry.McpHttpResource resource) {
        if (resource == null || resource.template) {
            return false
        }
        String method = resource.method ?: ''
        method == 'list' ||
            method == 'listAll' ||
            method == 'index' ||
            method.toLowerCase(Locale.ROOT).contains('list')
    }

    private static Map<String, String> queryParameters(Map<String, Object> arguments, int max, int offset) {
        Map<String, String> params = new LinkedHashMap<String, String>()
        params.put('max', String.valueOf(max))
        params.put('offset', String.valueOf(offset))
        addParam(params, 'sort', arguments.get('sort'))
        addParam(params, 'order', arguments.get('order'))
        addParam(params, 'label', arguments.get('label'))
        addParam(params, 'description', arguments.get('description'))
        addParam(params, 'code', arguments.get('code'))
        addParam(params, 'definition', arguments.get('definition'))
        addParam(params, 'domainType', arguments.get('domainType'))
        if (arguments.containsKey('all')) {
            addParam(params, 'all', arguments.get('all'))
        }
        Object filtersObj = arguments.get('filters')
        if (filtersObj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) filtersObj).entrySet()) {
                if (entry.key != null) {
                    addParam(params, String.valueOf(entry.key), entry.value)
                }
            }
        }
        params
    }

    private static void addParam(Map<String, String> params, String name, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            params.put(name, String.valueOf(value))
        }
    }

    private String pathFromResource(McpHttpResourceRegistry.McpHttpResource resource, Map<String, Object> arguments) {
        String path = asString(arguments.get('path'))
        if (path != null && !path.trim().isEmpty()) {
            return pathWithoutQuery(path)
        }
        resource.path
    }

    private static String appendQuery(String path, Map<String, String> params) {
        if (params.isEmpty()) {
            return path
        }
        String query = params.collect {String key, String value ->
            "${encode(key)}=${encode(value)}".toString()
        }.join('&')
        path.contains('?') ? path + '&' + query : path + '?' + query
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

    private static String summariseItem(Object item) {
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item
            List<String> parts = new ArrayList<String>()
            for (String key : ['label', 'domainType', 'id', 'description']) {
                Object value = map.get(key)
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    parts.add("${key}: ${String.valueOf(value).replace('\n', ' ')}".toString())
                }
            }
            return parts.isEmpty() ? JsonOutput.toJson(item) : parts.join('; ')
        }
        String.valueOf(item)
    }

    private static Map<String, Object> resourceToMap(McpHttpResourceRegistry.McpHttpResource resource) {
        [
            name: resource.name,
            uri: resource.resourceUri,
            path: resource.path,
            description: resource.description,
            mimeType: resource.mimeType,
            controller: resource.controller,
            action: resource.method
        ] as Map<String, Object>
    }

    private static String pathWithoutQuery(String path) {
        int queryAt = path.indexOf('?')
        queryAt >= 0 ? path.substring(0, queryAt) : path
    }

    private static String encode(String value) {
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
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

    private static Integer asIntegerOrNull(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue())
        }
        String text = String.valueOf(value)
        text.trim().isEmpty() ? null : Integer.valueOf(text)
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            target.put(key, value)
        }
    }
}
