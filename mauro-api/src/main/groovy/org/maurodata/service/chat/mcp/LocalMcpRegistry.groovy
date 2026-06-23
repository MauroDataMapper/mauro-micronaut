package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolSummaryDto

@CompileStatic
@Singleton
class LocalMcpRegistry {

    static final String LOCAL_SERVER_ID = 'local-mcp'
    private final Map<String, ToolHandler> handlersByName
    private final ResultGuidanceService resultGuidanceService

    LocalMcpRegistry(List<ToolHandler> handlers) {
        this(handlers, new ResultGuidanceService())
    }

    @Inject
    LocalMcpRegistry(List<ToolHandler> handlers, ResultGuidanceService resultGuidanceService) {
        this.handlersByName = handlers.collectEntries { ToolHandler handler ->
            [(handler.name()): handler]
        } as Map<String, ToolHandler>
        this.resultGuidanceService = resultGuidanceService
    }

    McpServerDto describeServer() {
        new McpServerDto(
            id: LOCAL_SERVER_ID,
            name: 'Local MCP',
            transport: 'STDIO',
            level: 'WORKSPACE',
            status: 'CONNECTED',
            tools: handlersByName.values().collect { ToolHandler handler ->
                new ToolSummaryDto(
                    name: handler.name(),
                    description: handler.description(),
                    inputSchema: handler.inputSchema() ?: [:],
                    routing: handler.routing() ?: [:],
                    annotations: handler.annotations() ?: [:]
                )
            }
        )
    }

    boolean hasTool(String toolName) {
        handlersByName.containsKey(toolName)
    }

    Map<String, Object> invoke(String toolName, Map<String, Object> arguments) {
        invokeDetailed(toolName, arguments).output
    }

    ToolInvocationResult invokeDetailed(String toolName, Map<String, Object> arguments) {
        ToolHandler handler = handlersByName[toolName]
        if (!handler) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Unsupported tool: ${toolName}")
        }
        Map<String, Object> output = handler.invoke(arguments ?: [:]) ?: [:]
        if (arguments != null && arguments.containsKey('withGuidance') && !output.containsKey('withGuidance')) {
            output.put('withGuidance', arguments.get('withGuidance'))
        }
        String modelText = handler.modelText(output)
        new ToolInvocationResult(
            output: output,
            modelText: resultGuidanceService != null ? resultGuidanceService.applyToolGuidance(toolName, output, modelText) : modelText
        )
    }
}
