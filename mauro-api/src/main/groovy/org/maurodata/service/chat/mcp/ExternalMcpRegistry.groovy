package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto

@CompileStatic
@Singleton
class ExternalMcpRegistry {

    List<McpServerDto> listServers() {
        Collections.<McpServerDto>emptyList()
    }

    boolean canHandle(String toolName) {
        toolName != null && toolName.contains('.')
    }

    Map<String, Object> invoke(String toolName, Map<String, Object> arguments) {
        throw new HttpStatusException(HttpStatus.BAD_REQUEST, "External MCP tool invocation is not implemented yet: ${toolName}")
    }
}
