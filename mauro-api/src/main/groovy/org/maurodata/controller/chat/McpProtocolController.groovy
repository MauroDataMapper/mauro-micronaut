package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.audit.Audit
import org.maurodata.service.chat.mcp.McpProtocolService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class McpProtocolController {

    private final McpProtocolService mcpProtocolService

    McpProtocolController(McpProtocolService mcpProtocolService) {
        this.mcpProtocolService = mcpProtocolService
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post('/mcp')
    HttpResponse<Object> post(@Body Object body) {
        Object response = mcpProtocolService.handle(body)
        response == null
            ? HttpResponse.accepted()
            : HttpResponse.ok(response)
    }
}
