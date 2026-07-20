package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.McpServerDto

@CompileStatic
@Singleton
class McpToolRegistry {

    private final LocalMcpRegistry localRegistry
    private final ExternalMcpRegistry externalRegistry

    McpToolRegistry(LocalMcpRegistry localRegistry, ExternalMcpRegistry externalRegistry) {
        this.localRegistry = localRegistry
        this.externalRegistry = externalRegistry
    }

    List<McpServerDto> listServers() {
        List<McpServerDto> servers = new ArrayList<McpServerDto>()
        servers.add(localRegistry.describeServer())
        servers.addAll(externalRegistry.listServers())
        servers
    }

    Map<String, Object> invoke(String toolName, Map<String, Object> arguments) {
        invokeDetailed(toolName, arguments).output
    }

    ToolInvocationResult invokeDetailed(String toolName, Map<String, Object> arguments) {
        String resolved = normalizeToolName(toolName)
        if (localRegistry.hasTool(resolved)) {
            return localRegistry.invokeDetailed(resolved, arguments)
        }
        if (externalRegistry.canHandle(resolved)) {
            return new ToolInvocationResult(output: externalRegistry.invoke(resolved, arguments))
        }
        return localRegistry.invokeDetailed(resolved, arguments)
    }

    String renderModelText(String toolName, Map<String, Object> output) {
        String resolved = normalizeToolName(toolName)
        if (localRegistry.hasTool(resolved)) {
            return localRegistry.renderModelText(resolved, output)
        }
        null
    }

    private static String normalizeToolName(String toolName) {
        if (toolName == null) {
            return null
        }
        int dot = toolName.indexOf('.')
        dot >= 0 ? toolName.substring(dot + 1) : toolName
    }
}
