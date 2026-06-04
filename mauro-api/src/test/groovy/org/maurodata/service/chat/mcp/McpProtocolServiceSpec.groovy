package org.maurodata.service.chat.mcp

import spock.lang.Specification

class McpProtocolServiceSpec extends Specification {

    TestToolHandler toolHandler = new TestToolHandler()
    McpToolRegistry mcpToolRegistry = new McpToolRegistry(
        new LocalMcpRegistry([toolHandler] as List<ToolHandler>),
        new ExternalMcpRegistry()
    )
    McpProtocolService service = new McpProtocolService(mcpToolRegistry)

    void 'initialize returns MCP server capabilities'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 1,
            method : 'initialize',
            params : [
                protocolVersion: '2025-03-26',
                clientInfo     : [
                    name   : 'spock',
                    version: '0.1.0'
                ]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 1
        response.result.protocolVersion == '2025-03-26'
        response.result.capabilities.tools.listChanged == false
        response.result.serverInfo.name == 'mauro-micronaut'
    }

    void 'initialized notification returns no response'() {
        expect:
        service.handle([
            jsonrpc: '2.0',
            method : 'notifications/initialized'
        ] as Map<String, Object>) == null
    }

    void 'tools list exposes registered tools and metadata'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'tools',
            method : 'tools/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'tools'
        response.result.tools.size() == 1
        response.result.tools[0].name == 'echo'
        response.result.tools[0].description == 'Echo tool'
        response.result.tools[0].inputSchema.type == 'object'
        response.result.tools[0]._meta.routing.useWhen == ['testing tool invocation']
    }

    void 'tools call invokes registry and returns text and structured content'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 2,
            method : 'tools/call',
            params : [
                name     : 'echo',
                arguments: [
                    message: 'hello'
                ]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 2
        response.result.isError == false
        response.result.content == [[type: 'text', text: 'Echoed hello']]
        response.result.structuredContent.echo.message == 'hello'
    }

    void 'unknown method returns json rpc method not found error'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 3,
            method : 'resources/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 3
        response.error.code == -32601
        response.error.message == 'Method not found: resources/list'
    }

    void 'tools call missing name returns invalid params error'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 4,
            method : 'tools/call',
            params : [
                arguments: [:]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 4
        response.error.code == -32602
        response.error.message == 'tools/call requires params.name'
    }

    void 'batch omits notification responses'() {
        when:
        List<Object> response = service.handle([
            [
                jsonrpc: '2.0',
                method : 'notifications/initialized'
            ],
            [
                jsonrpc: '2.0',
                id     : 5,
                method : 'tools/list',
                params : [:]
            ]
        ] as List<Object>) as List<Object>

        then:
        response.size() == 1
        (response[0] as Map<String, Object>).id == 5
    }

    static class TestToolHandler implements ToolHandler {

        @Override
        String name() {
            'echo'
        }

        @Override
        String description() {
            'Echo tool'
        }

        @Override
        Map<String, Object> inputSchema() {
            [type: 'object'] as Map<String, Object>
        }

        @Override
        Map<String, Object> routing() {
            [
                useWhen: ['testing tool invocation']
            ] as Map<String, Object>
        }

        @Override
        Map<String, Object> invoke(Map<String, Object> arguments) {
            [
                echo: arguments ?: [:]
            ] as Map<String, Object>
        }

        @Override
        String modelText(Map<String, Object> result) {
            'Echoed hello'
        }
    }
}
