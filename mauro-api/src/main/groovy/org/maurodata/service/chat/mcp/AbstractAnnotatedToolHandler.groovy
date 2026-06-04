package org.maurodata.service.chat.mcp

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractAnnotatedToolHandler implements ToolHandler {

    private final String toolName
    private final String toolDescription
    private final Map<String, Object> toolInputSchema
    private final Map<String, Object> toolRouting

    protected AbstractAnnotatedToolHandler(Class<?> metadataSource) {
        McpToolDefinition definition = metadataSource.getAnnotation(McpToolDefinition)
        if (definition == null) {
            throw new IllegalStateException("Missing @McpToolDefinition on ${metadataSource.name}")
        }
        this.toolName = definition.name()
        this.toolDescription = definition.description()
        this.toolInputSchema = parseSchema(definition.inputSchema())
        this.toolRouting = buildRouting(definition)
    }

    @Override
    final String name() {
        toolName
    }

    @Override
    final String description() {
        toolDescription
    }

    @Override
    final Map<String, Object> inputSchema() {
        toolInputSchema
    }

    @Override
    final Map<String, Object> routing() {
        toolRouting
    }

    @Override
    final Map<String, Object> invoke(Map<String, Object> arguments) {
        doInvoke(arguments ?: [:]) ?: [:]
    }

    @Override
    String modelText(Map<String, Object> result) {
        null
    }

    protected abstract Map<String, Object> doInvoke(Map<String, Object> arguments)

    private static Map<String, Object> buildRouting(McpToolDefinition definition) {
        Map<String, Object> routing = new LinkedHashMap<String, Object>()
        putIfPresent(routing, 'purpose', definition.purpose())
        putIfPresent(routing, 'useWhen', definition.useWhen())
        putIfPresent(routing, 'avoidWhen', definition.avoidWhen())
        putIfPresent(routing, 'examples', definition.examples())
        putIfPresent(routing, 'syntax', definition.syntax())
        putIfPresent(routing, 'filtering', definition.filtering())
        putIfPresent(routing, 'paging', definition.paging())
        putIfPresent(routing, 'limitations', definition.limitations())
        routing
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.put(key, value)
        }
    }

    private static void putIfPresent(Map<String, Object> target, String key, String[] values) {
        List<String> strings = new ArrayList<String>()
        for (String value : values ?: new String[0]) {
            if (value != null && !value.trim().isEmpty()) {
                strings.add(value)
            }
        }
        if (!strings.isEmpty()) {
            target.put(key, strings)
        }
    }

    private static Map<String, Object> parseSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            return [type: 'object'] as Map<String, Object>
        }
        Object parsed = new JsonSlurper().parseText(schemaJson)
        if (parsed instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> typed = (Map<String, Object>) parsed
            return typed
        }
        [type: 'object'] as Map<String, Object>
    }
}
