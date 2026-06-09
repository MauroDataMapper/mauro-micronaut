package org.maurodata.service.chat

import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse
import org.maurodata.api.chat.UpsertMcpServerRequest

interface ChatMcpService {
    List<McpServerDto> listServers()
    ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request)
    McpServerDto addServer(UpsertMcpServerRequest request)
    McpServerDto updateServer(String serverId, UpsertMcpServerRequest request)
    void removeServer(String serverId)
}
