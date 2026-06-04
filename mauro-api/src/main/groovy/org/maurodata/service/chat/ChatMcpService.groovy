package org.maurodata.service.chat

import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse

interface ChatMcpService {
    List<McpServerDto> listServers()
    ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request)
}
