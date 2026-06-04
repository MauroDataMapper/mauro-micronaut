package org.maurodata.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import jakarta.validation.Valid

@MauroApi
interface ChatMcpApi {

    @Get(Paths.CHAT_MCP_SERVERS)
    List<McpServerDto> listServers()

    @Post(Paths.CHAT_MCP_TOOL_INVOKE)
    ToolInvokeResponse invokeTool(@PathVariable String toolName, @Body @Valid ToolInvokeRequest request)
}
