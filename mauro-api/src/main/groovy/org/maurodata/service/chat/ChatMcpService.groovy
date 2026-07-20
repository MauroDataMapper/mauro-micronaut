package org.maurodata.service.chat

import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.plugin.chat.api.chat.UpsertMcpServerRequest

import groovy.transform.CompileStatic

@CompileStatic
interface ChatMcpService {
    List<McpServerDto> listServers()
    ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request)
    String renderModelText(String toolName, Map<String, Object> output)
    McpServerDto addServer(UpsertMcpServerRequest request)
    McpServerDto updateServer(String serverId, UpsertMcpServerRequest request)
    void removeServer(String serverId)
}
