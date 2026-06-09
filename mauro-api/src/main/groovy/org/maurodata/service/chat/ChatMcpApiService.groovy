package org.maurodata.service.chat

import groovy.util.logging.Slf4j
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse
import org.maurodata.api.chat.UpsertMcpServerRequest
import org.maurodata.service.chat.mcp.ExternalMcpRegistry
import org.maurodata.service.chat.mcp.McpToolRegistry
import org.maurodata.service.chat.mcp.ToolInvocationResult

import java.util.UUID

@Slf4j
@Singleton
class ChatMcpApiService implements ChatMcpService {

    private final McpToolRegistry mcpToolRegistry
    private final ExternalMcpRegistry externalMcpRegistry

    ChatMcpApiService(McpToolRegistry mcpToolRegistry, ExternalMcpRegistry externalMcpRegistry) {
        this.mcpToolRegistry = mcpToolRegistry
        this.externalMcpRegistry = externalMcpRegistry
    }

    @Override
    List<McpServerDto> listServers() {
        mcpToolRegistry.listServers()
    }

    @Override
    ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request) {
        long start = System.currentTimeMillis()
        String invocationId = UUID.randomUUID().toString()
        String nonce = UUID.randomUUID().toString().replace('-', '').substring(0, 12)
        log.info('invokeTool invocationId={} toolName={}', invocationId, toolName)
        try {
            ToolInvocationResult invocationResult = mcpToolRegistry.invokeDetailed(toolName, request?.arguments ?: [:])
            log.info('invokeTool nonce invocationId={} nonce={}', invocationId, nonce)
            new ToolInvokeResponse(
                success: true,
                result: [invocationId: invocationId, nonce: nonce, tool: toolName, output: invocationResult.output],
                modelText: invocationResult.modelText,
                error: null
            )
        } catch (HttpStatusException e) {
            throw e
        } finally {
            log.info('invokeTool completed invocationId={} toolName={} durationMs={}', invocationId, toolName, System.currentTimeMillis() - start)
        }
    }

    @Override
    McpServerDto addServer(UpsertMcpServerRequest request) {
        externalMcpRegistry.addServer(request)
    }

    @Override
    McpServerDto updateServer(String serverId, UpsertMcpServerRequest request) {
        externalMcpRegistry.updateServer(serverId, request)
    }

    @Override
    void removeServer(String serverId) {
        externalMcpRegistry.removeServer(serverId)
    }
}
