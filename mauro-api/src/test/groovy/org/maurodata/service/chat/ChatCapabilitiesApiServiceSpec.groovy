package org.maurodata.service.chat

import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.ModelDto
import org.maurodata.plugin.chat.api.chat.ProviderDto
import org.maurodata.service.chat.capabilities.CapabilitiesProvider
import spock.lang.Specification

class ChatCapabilitiesApiServiceSpec extends Specification {

    void 'capabilities do not expose skills and preserve explicit model capability flags'() {
        given:
        CapabilitiesProvider provider = Stub(CapabilitiesProvider) {
            providerId() >> 'test-provider'
            providerStatus() >> new ProviderDto(id: 'test-provider', status: 'SET')
            listModels() >> [
                new ModelDto(id: 'chat-model', provider: 'test-provider', streaming: true, tools: false),
                new ModelDto(id: 'embedding-model', provider: 'test-provider', streaming: false, tools: false)
            ]
        }
        ChatPromptAssetService promptAssetService = Stub(ChatPromptAssetService) {
            listAssetsByType('SKILL') >> [
                new ChatPromptAssetDefinition(id: 'internal-skill', name: 'Internal skill', type: 'SKILL')
            ]
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', transport: 'HTTP', url: '/mcp', status: 'CONNECTED')
            ]
        }
        ChatCapabilitiesApiService service = new ChatCapabilitiesApiService([provider], promptAssetService, mcpService)

        when:
        def capabilities = service.getCapabilities()

        then:
        capabilities.skills == []
        capabilities.models.find {it.id == 'chat-model'}.streaming == true
        capabilities.models.find {it.id == 'chat-model'}.tools == false
        capabilities.models.find {it.id == 'embedding-model'}.streaming == false
        capabilities.models.find {it.id == 'embedding-model'}.tools == false
        capabilities.mcpServers.first().transport == 'HTTP'
        capabilities.mcpServers.first().url == '/mcp'
    }
}
