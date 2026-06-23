package org.maurodata.controller.chat

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import org.maurodata.api.chat.SkillSummaryDto
import org.maurodata.service.chat.ChatSkillDefinition
import org.maurodata.service.chat.ChatSkillService
import org.maurodata.service.chat.mcp.ExternalMcpRegistry
import org.maurodata.service.chat.mcp.LocalMcpRegistry
import org.maurodata.service.chat.mcp.McpProtocolService
import org.maurodata.service.chat.mcp.McpToolRegistry
import org.maurodata.service.chat.mcp.ToolHandler
import spock.lang.Specification

class McpProtocolControllerSpec extends Specification {

    McpProtocolService mcpProtocolService = new McpProtocolService(
        new McpToolRegistry(
            new LocalMcpRegistry([new TestToolHandler()] as List<ToolHandler>),
            new ExternalMcpRegistry()
        ),
        new TestSkillService()
    )
    McpProtocolController controller = new McpProtocolController(mcpProtocolService)

    void 'post returns OK with JSON-RPC response body'() {
        given:
        Map<String, Object> request = [
            jsonrpc: '2.0',
            id     : 1,
            method : 'initialize',
            params : [:]
        ] as Map<String, Object>
        when:
        HttpResponse<Object> response = controller.post(HttpRequest.POST('/mcp', request), request)

        then:
        response.status == HttpStatus.OK
        Map<String, Object> body = response.body() as Map<String, Object>
        body.jsonrpc == '2.0'
        body.id == 1
        body.result.protocolVersion == '2025-03-26'
    }

    void 'post returns Accepted when protocol request is notification-only'() {
        given:
        Map<String, Object> request = [
            jsonrpc: '2.0',
            method : 'notifications/initialized'
        ] as Map<String, Object>

        when:
        HttpResponse<Object> response = controller.post(HttpRequest.POST('/mcp', request), request)

        then:
        response.status == HttpStatus.ACCEPTED
        !response.body()
    }

    void 'post supports batch response bodies'() {
        given:
        List<Object> request = [
            [
                jsonrpc: '2.0',
                method : 'notifications/initialized'
            ],
            [
                jsonrpc: '2.0',
                id     : 2,
                method : 'tools/list',
                params : [:]
            ]
        ] as List<Object>
        when:
        HttpResponse<Object> response = controller.post(HttpRequest.POST('/mcp', request), request)

        then:
        response.status == HttpStatus.OK
        List<Object> body = response.body() as List<Object>
        body.size() == 1
        (body[0] as Map<String, Object>).id == 2
        ((body[0] as Map<String, Object>).result as Map<String, Object>).tools
    }

    static class TestToolHandler implements ToolHandler {

        @Override
        String name() {
            'echo'
        }

        @Override
        String description() {
            'Echo tool'
        }

        @Override
        Map<String, Object> inputSchema() {
            [type: 'object'] as Map<String, Object>
        }

        @Override
        Map<String, Object> routing() {
            [
                useWhen: ['testing tool invocation']
            ] as Map<String, Object>
        }

        @Override
        Map<String, Object> annotations() {
            [
                readOnlyHint   : true,
                destructiveHint: false,
                idempotentHint : true,
                openWorldHint  : false
            ] as Map<String, Object>
        }

        @Override
        Map<String, Object> invoke(Map<String, Object> arguments) {
            [
                echo: arguments ?: [:]
            ] as Map<String, Object>
        }

        @Override
        String modelText(Map<String, Object> result) {
            'Echoed hello'
        }
    }

    static class TestSkillService implements ChatSkillService {

        List<ChatSkillDefinition> skills = [
            new ChatSkillDefinition(
                id: 'mauro-catalogue',
                name: 'Mauro Catalogue',
                description: 'Catalogue persona',
                scope: 'GLOBAL',
                version: '1.0.0',
                type: 'PERSONA',
                priority: 0,
                instruction: 'You are Mauro catalogue assistant.'
            )
        ] as List<ChatSkillDefinition>

        @Override
        List<SkillSummaryDto> listSkills() {
            skills.collect {ChatSkillDefinition skill ->
                new SkillSummaryDto(
                    id: skill.id,
                    name: skill.name,
                    description: skill.description,
                    scope: skill.scope,
                    version: skill.version
                )
            }
        }

        @Override
        List<ChatSkillDefinition> listSkillDefinitions() {
            skills
        }

        @Override
        List<ChatSkillDefinition> listPersonaDefinitions() {
            skills.findAll {ChatSkillDefinition skill -> 'PERSONA'.equalsIgnoreCase(skill.type)}
        }

        @Override
        ChatSkillDefinition findSkill(String id) {
            skills.find {ChatSkillDefinition skill -> skill.id == id}
        }

        @Override
        List<ChatSkillDefinition> searchSkills(String query) {
            skills
        }
    }
}
