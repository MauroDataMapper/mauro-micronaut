package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.validation.Valid
import org.maurodata.api.Paths
import org.maurodata.api.chat.ChatMcpApi
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse
import org.maurodata.audit.Audit
import org.maurodata.service.chat.ChatMcpService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ChatMcpController implements ChatMcpApi {

    private final ChatMcpService chatMcpService

    ChatMcpController(ChatMcpService chatMcpService) {
        this.chatMcpService = chatMcpService
    }

    @Override
    @Audit
    @Get(Paths.CHAT_MCP_SERVERS)
    List<McpServerDto> listServers() {
        chatMcpService.listServers()
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.CHAT_MCP_TOOL_INVOKE)
    ToolInvokeResponse invokeTool(@PathVariable String toolName, @Body @Valid ToolInvokeRequest request) {
        chatMcpService.invokeTool(toolName, request)
    }
}
