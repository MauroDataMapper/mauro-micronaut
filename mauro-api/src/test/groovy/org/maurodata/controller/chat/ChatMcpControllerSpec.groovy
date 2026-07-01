package org.maurodata.controller.chat

import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.plugin.chat.api.chat.UpsertMcpServerRequest
import org.maurodata.service.chat.ChatMcpService
import spock.lang.Specification

class ChatMcpControllerSpec extends Specification {

    TestChatMcpService service = new TestChatMcpService()
    ChatMcpController controller = new ChatMcpController(service)

    void 'add server delegates to chat mcp service'() {
        when:
        McpServerDto response = controller.addServer(new UpsertMcpServerRequest(
            name: 'Docs',
            url: 'http://localhost:9000/mcp'
        ))

        then:
        response.id == 'generated'
        response.name == 'Docs'
        response.transport == 'HTTP'
        response.url == 'http://localhost:9000/mcp'
        service.added.url == 'http://localhost:9000/mcp'
    }

    void 'update server delegates to chat mcp service'() {
        when:
        McpServerDto response = controller.updateServer('docs', new UpsertMcpServerRequest(
            name: 'Docs Updated',
            url: 'https://example.test/mcp'
        ))

        then:
        response.id == 'docs'
        response.name == 'Docs Updated'
        response.url == 'https://example.test/mcp'
        service.updatedId == 'docs'
    }

    void 'remove server delegates to chat mcp service'() {
        when:
        controller.removeServer('docs')

        then:
        service.removedId == 'docs'
    }

    static class TestChatMcpService implements ChatMcpService {
        UpsertMcpServerRequest added
        String updatedId
        String removedId

        @Override
        List<McpServerDto> listServers() {
            []
        }

        @Override
        ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request) {
            null
        }

        @Override
        McpServerDto addServer(UpsertMcpServerRequest request) {
            added = request
            new McpServerDto(id: request.id ?: 'generated', name: request.name, transport: 'HTTP', url: request.url)
        }

        @Override
        McpServerDto updateServer(String serverId, UpsertMcpServerRequest request) {
            updatedId = serverId
            new McpServerDto(id: serverId, name: request.name, transport: 'HTTP', url: request.url)
        }

        @Override
        void removeServer(String serverId) {
            removedId = serverId
        }
    }
}
