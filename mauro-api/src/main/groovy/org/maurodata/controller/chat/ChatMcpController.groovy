package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.validation.Valid
import org.maurodata.api.Paths
import org.maurodata.api.chat.ChatMcpApi
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.UpsertMcpServerRequest
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
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.CHAT_MCP_SERVERS)
    McpServerDto addServer(@Body @Valid UpsertMcpServerRequest request) {
        chatMcpService.addServer(request)
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.CHAT_MCP_SERVER)
    McpServerDto updateServer(@PathVariable String serverId, @Body @Valid UpsertMcpServerRequest request) {
        chatMcpService.updateServer(serverId, request)
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Status(HttpStatus.NO_CONTENT)
    @Delete(Paths.CHAT_MCP_SERVER)
    void removeServer(@PathVariable String serverId) {
        chatMcpService.removeServer(serverId)
    }
}
