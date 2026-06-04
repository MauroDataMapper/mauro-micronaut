package org.maurodata.service.chat.mcp

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolSummaryDto

@CompileStatic
@Singleton
class McpProtocolService {

    static final String SUPPORTED_PROTOCOL_VERSION = '2025-03-26'

    private final McpToolRegistry mcpToolRegistry

    McpProtocolService(McpToolRegistry mcpToolRegistry) {
        this.mcpToolRegistry = mcpToolRegistry
    }

    Object handle(Object requestBody) {
        if (requestBody instanceof List) {
            List<Object> responses = new ArrayList<Object>()
            for (Object item : (List<?>) requestBody) {
                Object response = handleSingle(item)
                if (response != null) {
                    responses.add(response)
                }
            }
            return responses.isEmpty() ? null : responses
        }
        handleSingle(requestBody)
    }

    private Object handleSingle(Object requestBody) {
        if (!(requestBody instanceof Map)) {
            return error(null, -32600, 'Invalid Request')
        }

        @SuppressWarnings('unchecked')
        Map<String, Object> request = (Map<String, Object>) requestBody
        Object id = request.get('id')
        String method = asString(request.get('method'))
        boolean notification = !request.containsKey('id')

        if (request.get('jsonrpc') != '2.0' || method == null || method.trim().isEmpty()) {
            return notification ? null : error(id, -32600, 'Invalid Request')
        }

        try {
            switch (method) {
                case 'initialize':
                    return success(id, initializeResult(request))
                case 'notifications/initialized':
                    return null
                case 'ping':
                    return notification ? null : success(id, [:] as Map<String, Object>)
                case 'tools/list':
                    return notification ? null : success(id, toolsListResult())
                case 'tools/call':
                    return notification ? null : success(id, callToolResult(request))
                default:
                    return notification ? null : error(id, -32601, "Method not found: ${method}")
            }
        } catch (IllegalArgumentException e) {
            return notification ? null : error(id, -32602, e.message ?: 'Invalid params')
        } catch (Throwable t) {
            return notification ? null : error(id, -32603, t.message ?: 'Internal error')
        }
    }

    private static Map<String, Object> initializeResult(Map<String, Object> request) {
        Map<String, Object> params = getMap(request.get('params'))
        String requestedVersion = asString(params.get('protocolVersion'))
        [
            protocolVersion: requestedVersion ?: SUPPORTED_PROTOCOL_VERSION,
            capabilities   : [
                tools: [
                    listChanged: false
                ] as Map<String, Object>
            ] as Map<String, Object>,
            serverInfo     : [
                name   : 'mauro-micronaut',
                title  : 'Mauro Micronaut MCP Server',
                version: '0.1.0'
            ] as Map<String, Object>,
            instructions   : 'Mauro catalogue tools for searching and understanding catalogue content.'
        ] as Map<String, Object>
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>()
        for (McpServerDto server : mcpToolRegistry.listServers()) {
            for (ToolSummaryDto tool : server.tools ?: [] as List<ToolSummaryDto>) {
                Map<String, Object> mcpTool = [
                    name       : tool.name,
                    description: tool.description ?: '',
                    inputSchema: tool.inputSchema ?: [type: 'object'] as Map<String, Object>
                ] as Map<String, Object>
                if (tool.routing != null && !tool.routing.isEmpty()) {
                    mcpTool.put('_meta', [
                        routing: tool.routing
                    ] as Map<String, Object>)
                }
                tools.add(mcpTool)
            }
        }
        [
            tools: tools
        ] as Map<String, Object>
    }

    private Map<String, Object> callToolResult(Map<String, Object> request) {
        Map<String, Object> params = getMap(request.get('params'))
        String name = asString(params.get('name'))
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException('tools/call requires params.name')
        }
        Map<String, Object> arguments = getMap(params.get('arguments'))

        ToolInvocationResult invocationResult = mcpToolRegistry.invokeDetailed(name, arguments)
        String text = invocationResult.modelText
        if (text == null || text.trim().isEmpty()) {
            text = JsonOutput.toJson(invocationResult.output ?: [:])
        }

        [
            content: [
                [
                    type: 'text',
                    text: text
                ] as Map<String, Object>
            ],
            structuredContent: invocationResult.output ?: [:],
            isError: false
        ] as Map<String, Object>
    }

    private static Map<String, Object> success(Object id, Map<String, Object> result) {
        [
            jsonrpc: '2.0',
            id     : id,
            result : result
        ] as Map<String, Object>
    }

    private static Map<String, Object> error(Object id, int code, String message) {
        [
            jsonrpc: '2.0',
            id     : id,
            error  : [
                code   : code,
                message: message
            ] as Map<String, Object>
        ] as Map<String, Object>
    }

    private static Map<String, Object> getMap(Object value) {
        if (value == null) {
            return [:] as Map<String, Object>
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException('Expected JSON object')
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> typed = (Map<String, Object>) value
        typed
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}
