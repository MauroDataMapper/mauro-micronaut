package org.maurodata.service.chat.mcp

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.ToolSummaryDto
import org.maurodata.service.chat.ChatSkillDefinition
import org.maurodata.service.chat.ChatSkillService
import org.maurodata.service.chat.SkillRouting
import org.maurodata.service.chat.SkillToolApplicability

@CompileStatic
@Slf4j
@Singleton
class McpProtocolService {

    static final String SUPPORTED_PROTOCOL_VERSION = '2025-03-26'

    private final McpToolRegistry mcpToolRegistry
    private final ChatSkillService chatSkillService

    McpProtocolService(McpToolRegistry mcpToolRegistry, ChatSkillService chatSkillService) {
        this.mcpToolRegistry = mcpToolRegistry
        this.chatSkillService = chatSkillService
    }

    Object handle(Object requestBody) {
        if (requestBody instanceof List) {
            List<Object> responses = new ArrayList<Object>()
            for (Object item : (List<?>) requestBody) {
                Object response = handleSingle(item)
                if (response != null) {
                    responses.add(response)
                }
            }
            return responses.isEmpty() ? null : responses
        }
        handleSingle(requestBody)
    }

    private Object handleSingle(Object requestBody) {
        if (!(requestBody instanceof Map)) {
            return error(null, -32600, 'Invalid Request')
        }

        @SuppressWarnings('unchecked')
        Map<String, Object> request = (Map<String, Object>) requestBody
        Object id = request.get('id')
        String method = asString(request.get('method'))
        boolean notification = !request.containsKey('id')

        if (request.get('jsonrpc') != '2.0' || method == null || method.trim().isEmpty()) {
            return notification ? null : error(id, -32600, 'Invalid Request')
        }

        try {
            switch (method) {
                case 'initialize':
                    return success(id, initializeResult(request))
                case 'notifications/initialized':
                    return null
                case 'ping':
                    return notification ? null : success(id, [:] as Map<String, Object>)
                case 'tools/list':
                    return notification ? null : success(id, toolsListResult())
                case 'tools/call':
                    return notification ? null : success(id, callToolResult(request))
                case 'prompts/list':
                    return notification ? null : success(id, promptsListResult())
                case 'prompts/get':
                    return notification ? null : success(id, getPromptResult(request))
                default:
                    return notification ? null : error(id, -32601, "Method not found: ${method}")
            }
        } catch (IllegalArgumentException e) {
            return notification ? null : error(id, -32602, e.message ?: 'Invalid params')
        } catch (Throwable t) {
            log.error('MCP method failed method={} id={}', method, id, t)
            return notification ? null : error(id, -32603, t.message ?: 'Internal error')
        }
    }

    private static Map<String, Object> initializeResult(Map<String, Object> request) {
        Map<String, Object> params = getMap(request.get('params'))
        String requestedVersion = asString(params.get('protocolVersion'))
        [
            protocolVersion: requestedVersion ?: SUPPORTED_PROTOCOL_VERSION,
            capabilities   : [
                tools: [
                    listChanged: false
                ] as Map<String, Object>,
                prompts: [
                    listChanged: false
                ] as Map<String, Object>
            ] as Map<String, Object>,
            serverInfo     : [
                name   : 'mauro-micronaut',
                title  : 'Mauro Micronaut MCP Server',
                version: '0.1.0'
            ] as Map<String, Object>,
            instructions   : 'Mauro catalogue tools for searching and understanding catalogue content.'
        ] as Map<String, Object>
    }

    private Map<String, Object> promptsListResult() {
        List<Map<String, Object>> prompts = new ArrayList<Map<String, Object>>()
        prompts.add([
            name       : 'mauro-assistant-context',
            title      : 'Mauro Assistant Context',
            description: 'Persona, routing, and tool prerequisite guidance for using Mauro MCP tools with an external model.'
        ] as Map<String, Object>)

        for (ChatSkillDefinition skill : sortedSkills()) {
            if (skill == null || skill.id == null || skill.id.trim().isEmpty()) {
                log.warn('Skipping MCP prompt for chat skill with missing id: {}', skill)
                continue
            }
            prompts.add([
                name       : skill.id,
                title      : skill.name ?: skill.id,
                description: skill.description ?: ''
            ] as Map<String, Object>)
        }
        [
            prompts: prompts
        ] as Map<String, Object>
    }

    private Map<String, Object> getPromptResult(Map<String, Object> request) {
        Map<String, Object> params = getMap(request.get('params'))
        String name = asString(params.get('name'))
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException('prompts/get requires params.name')
        }

        if (name == 'mauro-assistant-context') {
            return [
                description: 'Mauro assistant context for external MCP clients.',
                messages   : [
                    promptMessage(buildAssistantContextPrompt())
                ]
            ] as Map<String, Object>
        }

        ChatSkillDefinition skill = chatSkillService.findSkill(name)
        if (skill == null) {
            throw new IllegalArgumentException("Unknown prompt: ${name}")
        }
        [
            description: skill.description ?: skill.name,
            messages   : [
                promptMessage(buildSkillPrompt(skill))
            ]
        ] as Map<String, Object>
    }

    private String buildAssistantContextPrompt() {
        StringBuilder builder = new StringBuilder(4096)
        builder.append('Use the following Mauro assistant context when working with this MCP server. ')
            .append('If your client supports system or developer messages, this content is suitable for that role; otherwise include it as high-priority conversation context.')
            .append('\n\n')

        List<ChatSkillDefinition> personas = chatSkillService.listPersonaDefinitions()
        if (!personas.isEmpty()) {
            builder.append('## Persona\n')
            for (ChatSkillDefinition persona : personas) {
                if (persona.instruction != null && !persona.instruction.trim().isEmpty()) {
                    builder.append(persona.instruction.trim())
                        .append('\n\n')
                }
            }
        }

        builder.append('## Skill routes\n')
        builder.append('Retrieve the most specific relevant skill prompt before using tools when domain context is needed.\n')
        for (ChatSkillDefinition skill : sortedSkills()) {
            if ('PERSONA'.equalsIgnoreCase(skill.type)) {
                continue
            }
            builder.append('- ')
                .append(skill.id)
                .append(': ')
                .append(skill.description ?: skill.name)
            SkillRouting routing = skill.routing
            if (routing != null && !(routing.useWhen ?: []).isEmpty()) {
                builder.append(' Use when: ')
                    .append(routing.useWhen.join('; '))
                    .append('.')
            }
            if (routing != null && !(routing.avoidWhen ?: []).isEmpty()) {
                builder.append(' Avoid when: ')
                    .append(routing.avoidWhen.join('; '))
                    .append('.')
            }
            if (!(skill.seeAlso ?: []).isEmpty()) {
                builder.append(' See also: ')
                    .append(skill.seeAlso.join(', '))
                    .append('.')
            }
            builder.append('\n')
        }

        List<String> applicabilityRoutes = buildToolApplicabilityRoutes()
        if (!applicabilityRoutes.isEmpty()) {
            builder.append('\n## Tool prerequisite/context routes\n')
            builder.append('Before calling a tool, check these skill-owned routes. REQUIRED_PREREQUISITE skills must be retrieved first when their useWhen matches. RECOMMENDED_PREREQUISITE and RECOMMENDED_CONTEXT skills should be retrieved when their useWhen matches unless the needed context is already present.\n')
            for (String route : applicabilityRoutes) {
                builder.append('- ')
                    .append(route)
                    .append('\n')
            }
        }

        builder.append('\n## Available tools\n')
        for (McpServerDto server : mcpToolRegistry.listServers()) {
            for (ToolSummaryDto tool : server.tools ?: [] as List<ToolSummaryDto>) {
                builder.append('- ')
                    .append(tool.name)
                    .append(': ')
                    .append(tool.description ?: '')
                    .append('\n')
            }
        }
        builder.toString().trim()
    }

    private String buildSkillPrompt(ChatSkillDefinition skill) {
        StringBuilder builder = new StringBuilder(2048)
        builder.append('Use this Mauro skill guidance when it is relevant to the user request.')
            .append('\n\n')
            .append('# ')
            .append(skill.name)
            .append('\n\n')
            .append(skill.description ?: '')
            .append('\n\n')
        if (!(skill.seeAlso ?: []).isEmpty()) {
            builder.append('See also skills: ')
                .append(skill.seeAlso.join(', '))
                .append('\n\n')
        }
        if (skill.instruction != null && !skill.instruction.trim().isEmpty()) {
            builder.append(skill.instruction.trim())
        }
        builder.toString().trim()
    }

    private List<String> buildToolApplicabilityRoutes() {
        List<String> routes = new ArrayList<String>()
        for (ChatSkillDefinition skill : sortedSkills()) {
            if ('PERSONA'.equalsIgnoreCase(skill.type)) {
                continue
            }
            for (SkillToolApplicability applicability : skill.toolApplicability ?: []) {
                if (applicability == null || applicability.tool == null || applicability.tool.trim().isEmpty()) {
                    continue
                }
                StringBuilder route = new StringBuilder(512)
                route.append(applicability.tool)
                    .append(' <- ')
                    .append(skill.id)
                    .append(' (')
                    .append(applicability.relationship ?: 'RECOMMENDED_PREREQUISITE')
                    .append(')')
                if (!(applicability.useWhen ?: []).isEmpty()) {
                    route.append(' Use when: ')
                        .append(applicability.useWhen.join('; '))
                        .append('.')
                }
                if (!(applicability.instructions ?: []).isEmpty()) {
                    route.append(' Instructions: ')
                        .append(applicability.instructions.join('; '))
                        .append('.')
                }
                routes.add(route.toString())
            }
        }
        routes
    }

    private List<ChatSkillDefinition> sortedSkills() {
        List<ChatSkillDefinition> skills = new ArrayList<ChatSkillDefinition>(chatSkillService.listSkillDefinitions() ?: [])
        skills
            .sort {ChatSkillDefinition left, ChatSkillDefinition right ->
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left.id ?: '') <=> (right.id ?: '')
            } as List<ChatSkillDefinition>
    }

    private static Map<String, Object> promptMessage(String text) {
        [
            role   : 'user',
            content: [
                type: 'text',
                text: text
            ] as Map<String, Object>
        ] as Map<String, Object>
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>()
        for (McpServerDto server : mcpToolRegistry.listServers()) {
            for (ToolSummaryDto tool : server.tools ?: [] as List<ToolSummaryDto>) {
                Map<String, Object> mcpTool = [
                    name       : tool.name,
                    description: tool.description ?: '',
                    inputSchema: tool.inputSchema ?: [type: 'object'] as Map<String, Object>
                ] as Map<String, Object>
                if (tool.annotations != null && !tool.annotations.isEmpty()) {
                    mcpTool.put('annotations', tool.annotations)
                }
                if (tool.routing != null && !tool.routing.isEmpty()) {
                    mcpTool.put('_meta', [
                        routing: tool.routing
                    ] as Map<String, Object>)
                }
                tools.add(mcpTool)
            }
        }
        [
            tools: tools
        ] as Map<String, Object>
    }

    private Map<String, Object> callToolResult(Map<String, Object> request) {
        Map<String, Object> params = getMap(request.get('params'))
        String name = asString(params.get('name'))
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException('tools/call requires params.name')
        }
        Map<String, Object> arguments = getMap(params.get('arguments'))

        ToolInvocationResult invocationResult = mcpToolRegistry.invokeDetailed(name, arguments)
        String text = invocationResult.modelText
        if (text == null || text.trim().isEmpty()) {
            text = JsonOutput.toJson(invocationResult.output ?: [:])
        }

        [
            content: [
                [
                    type: 'text',
                    text: text
                ] as Map<String, Object>
            ],
            structuredContent: invocationResult.output ?: [:],
            isError: false
        ] as Map<String, Object>
    }

    private static Map<String, Object> success(Object id, Map<String, Object> result) {
        [
            jsonrpc: '2.0',
            id     : id,
            result : result
        ] as Map<String, Object>
    }

    private static Map<String, Object> error(Object id, int code, String message) {
        [
            jsonrpc: '2.0',
            id     : id,
            error  : [
                code   : code,
                message: message
            ] as Map<String, Object>
        ] as Map<String, Object>
    }

    private static Map<String, Object> getMap(Object value) {
        if (value == null) {
            return [:] as Map<String, Object>
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException('Expected JSON object')
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> typed = (Map<String, Object>) value
        typed
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}
