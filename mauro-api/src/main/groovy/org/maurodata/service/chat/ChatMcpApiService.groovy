package org.maurodata.service.chat

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.plugin.chat.api.chat.ToolSummaryDto
import org.maurodata.plugin.chat.api.chat.UpsertMcpServerRequest
import org.maurodata.service.chat.mcp.ExternalMcpRegistry
import org.maurodata.service.chat.mcp.McpToolRegistry
import org.maurodata.service.chat.mcp.ToolInvocationResult

import java.util.UUID

@Slf4j
@Singleton
class ChatMcpApiService implements ChatMcpService {

    private final McpToolRegistry mcpToolRegistry
    private final ExternalMcpRegistry externalMcpRegistry

    ChatMcpApiService(McpToolRegistry mcpToolRegistry, ExternalMcpRegistry externalMcpRegistry) {
        this.mcpToolRegistry = mcpToolRegistry
        this.externalMcpRegistry = externalMcpRegistry
    }

    @Override
    List<McpServerDto> listServers() {
        mcpToolRegistry.listServers()
    }

    @Override
    ToolInvokeResponse invokeTool(String toolName, ToolInvokeRequest request) {
        long start = System.currentTimeMillis()
        String invocationId = UUID.randomUUID().toString()
        String nonce = UUID.randomUUID().toString().replace('-', '').substring(0, 12)
        log.info('invokeTool invocationId={} toolName={}', invocationId, toolName)
        try {
            Map<String, Object> arguments = new LinkedHashMap<String, Object>(request?.arguments ?: [:])
            if (request?.forwardHeaders != null && !request.forwardHeaders.isEmpty()) {
                arguments.put('_mauroForwardHeaders', request.forwardHeaders)
            }
            ToolInvocationResult invocationResult = mcpToolRegistry.invokeDetailed(toolName, arguments)
            log.info('invokeTool nonce invocationId={} nonce={}', invocationId, nonce)
            new ToolInvokeResponse(
                success: true,
                result: [invocationId: invocationId, nonce: nonce, tool: toolName, output: invocationResult.output],
                modelText: invocationResult.modelText,
                error: null
            )
        } catch (HttpStatusException e) {
            if (e.status == HttpStatus.BAD_REQUEST && e.message?.startsWith('Unsupported tool:')) {
                return unknownToolResponse(toolName, invocationId, nonce)
            }
            throw e
        } finally {
            log.info('invokeTool completed invocationId={} toolName={} durationMs={}', invocationId, toolName, System.currentTimeMillis() - start)
        }
    }

    @Override
    McpServerDto addServer(UpsertMcpServerRequest request) {
        externalMcpRegistry.addServer(request)
    }

    @Override
    McpServerDto updateServer(String serverId, UpsertMcpServerRequest request) {
        externalMcpRegistry.updateServer(serverId, request)
    }

    @Override
    void removeServer(String serverId) {
        externalMcpRegistry.removeServer(serverId)
    }

    private ToolInvokeResponse unknownToolResponse(String toolName, String invocationId, String nonce) {
        List<String> availableTools = availableToolNames()
        String guidance = buildUnknownToolModelText(toolName, availableTools)
        new ToolInvokeResponse(
            success: false,
            result: [
                invocationId : invocationId,
                nonce        : nonce,
                tool         : toolName,
                error        : "Unknown tool: ${toolName}".toString(),
                availableTools: availableTools
            ] as Map<String, Object>,
            modelText: guidance,
            error: "Unknown tool: ${toolName}".toString()
        )
    }

    private List<String> availableToolNames() {
        List<String> names = new ArrayList<String>()
        for (McpServerDto server : mcpToolRegistry.listServers()) {
            for (ToolSummaryDto tool : server.tools ?: [] as List<ToolSummaryDto>) {
                if (tool?.name != null && !tool.name.trim().isEmpty() && !names.contains(tool.name)) {
                    names.add(tool.name)
                }
            }
        }
        names.sort()
    }

    private static String buildUnknownToolModelText(String toolName, List<String> availableTools) {
        List<String> guidance = [
            "The requested tool '${toolName}' is not a registered callable tool.".toString(),
            'Do not retry the same unknown tool call.',
            'Use one of the available callable tool names listed below.'
        ] as List<String>
        if (toolName != null && toolName.startsWith('mauro-')) {
            guidance.add("The name '${toolName}' looks like a skill id, not a tool name.".toString())
            guidance.add("If this skill context is needed, call mauro_skill with arguments {\"id\":\"${toolName}\",\"includeInstruction\":true}.".toString())
        }
        guidance.add('If a catalogue item id and matching resource URI are already known, call mauro_get directly.')

        renderModelTextSections([
            'Tool Call Status'   : ['Tool invocation failed because the tool name was not recognised.'],
            'Result Metadata'    : [
                "Requested tool: ${toolName ?: ''}",
                "Available callable tools: ${availableTools.join(', ')}"
            ],
            'Correction Guidance': guidance,
            'Completion Guidance': [
                'Choose the correct registered tool and continue the user request.',
                'If enough information is already available to answer, answer directly instead of calling another tool.'
            ]
        ] as Map<String, Object>)
    }

    private static String renderModelTextSections(Map<String, ?> sections) {
        StringBuilder builder = new StringBuilder(1024)
        for (Map.Entry<String, ?> entry : sections.entrySet()) {
            List<String> lines = normalizeSectionLines(entry.value)
            if (lines.isEmpty()) {
                continue
            }
            if (builder.length() > 0) {
                builder.append('\n\n')
            }
            builder.append('## ')
                .append(entry.key)
                .append('\n')
            for (String line : lines) {
                builder.append(line)
                if (!line.endsWith('\n')) {
                    builder.append('\n')
                }
            }
        }
        builder.toString()
    }

    private static List<String> normalizeSectionLines(Object value) {
        if (value == null) {
            return Collections.emptyList()
        }
        if (value instanceof Collection) {
            List<String> lines = new ArrayList<String>()
            for (Object item : (Collection<?>) value) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    lines.add(String.valueOf(item))
                }
            }
            return lines
        }
        String text = String.valueOf(value)
        if (text.trim().isEmpty()) {
            return Collections.emptyList()
        }
        List<String> lines = new ArrayList<String>()
        for (String line : text.split(/\r?\n/)) {
            if (!line.trim().isEmpty()) {
                lines.add(line)
            }
        }
        lines
    }
}
