package org.maurodata.service.chat.mcp

import org.maurodata.plugin.chat.api.chat.SkillSummaryDto
import org.maurodata.service.chat.ChatSkillDefinition
import org.maurodata.service.chat.ChatSkillDefinitionLoader
import org.maurodata.service.chat.ChatSkillService
import org.maurodata.service.chat.ChatSkillsRegistryService
import org.maurodata.service.chat.SkillRouting
import org.maurodata.service.chat.SkillToolApplicability
import jakarta.inject.Inject
import spock.lang.Specification

class McpProtocolServiceSpec extends Specification {

    TestToolHandler toolHandler = new TestToolHandler()
    McpToolRegistry mcpToolRegistry = new McpToolRegistry(
        new LocalMcpRegistry([toolHandler] as List<ToolHandler>),
        new ExternalMcpRegistry()
    )
    TestSkillService skillService = new TestSkillService()
    McpProtocolService service = new McpProtocolService(mcpToolRegistry, skillService)

    void 'production constructor is injectable with resource dependencies'() {
        expect:
        McpProtocolService.declaredConstructors.any {constructor ->
            constructor.parameterTypes.toList() == [
                McpToolRegistry,
                ChatSkillService,
                McpHttpResourceRegistry,
                io.micronaut.runtime.server.EmbeddedServer
            ] && constructor.getAnnotation(Inject) != null
        }
    }

    void 'initialize returns MCP server capabilities'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 1,
            method : 'initialize',
            params : [
                protocolVersion: '2025-03-26',
                clientInfo     : [
                    name   : 'spock',
                    version: '0.1.0'
                ]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 1
        response.result.protocolVersion == '2025-03-26'
        response.result.capabilities.tools.listChanged == false
        response.result.capabilities.prompts.listChanged == false
        response.result.serverInfo.name == 'mauro-micronaut'
    }

    void 'initialized notification returns no response'() {
        expect:
        service.handle([
            jsonrpc: '2.0',
            method : 'notifications/initialized'
        ] as Map<String, Object>) == null
    }

    void 'tools list exposes registered tools and metadata'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'tools',
            method : 'tools/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'tools'
        response.result.tools.size() == 1
        response.result.tools[0].name == 'echo'
        response.result.tools[0].description == 'Echo tool'
        response.result.tools[0].inputSchema.type == 'object'
        response.result.tools[0].annotations.readOnlyHint == true
        response.result.tools[0].annotations.destructiveHint == false
        response.result.tools[0].annotations.idempotentHint == true
        response.result.tools[0].annotations.openWorldHint == false
        response.result.tools[0]._meta.routing.useWhen == ['testing tool invocation']
    }

    void 'tools call invokes registry and returns text and structured content'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 2,
            method : 'tools/call',
            params : [
                name     : 'echo',
                arguments: [
                    message: 'hello'
                ]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 2
        response.result.isError == false
        response.result.content == [[type: 'text', text: 'Echoed hello']]
        response.result.structuredContent.echo.message == 'hello'
    }

    void 'prompts list exposes assistant context and skills'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'prompts',
            method : 'prompts/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'prompts'
        response.result.prompts*.name == ['mauro-assistant-context', 'mauro-catalogue', 'mauro-glossary']
        response.result.prompts[0].title == 'Mauro Assistant Context'
    }

    void 'prompts list works with real skill definitions'() {
        given:
        McpProtocolService realSkillService = new McpProtocolService(
            mcpToolRegistry,
            new ChatSkillsRegistryService(new ChatSkillDefinitionLoader())
        )

        when:
        Map<String, Object> response = realSkillService.handle([
            jsonrpc: '2.0',
            id     : 'real-prompts',
            method : 'prompts/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'real-prompts'
        !response.error
        response.result.prompts*.name.contains('mauro-assistant-context')
        response.result.prompts*.name.contains('mauro-catalogue')
        response.result.prompts*.name.contains('mauro-glossary')
    }

    void 'prompts get returns assistant context prompt with persona routing and tools'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'context',
            method : 'prompts/get',
            params : [
                name: 'mauro-assistant-context'
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'context'
        response.result.description == 'Mauro assistant context for external MCP clients.'
        response.result.messages[0].role == 'user'
        String text = response.result.messages[0].content.text
        text.contains('## Persona')
        text.contains('## Skill routes')
        text.contains('mauro_keyword_search <- mauro-glossary')
        text.contains('## Available tools')
        text.contains('echo: Echo tool')
    }

    void 'prompts get returns skill instruction prompt'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'skill',
            method : 'prompts/get',
            params : [
                name: 'mauro-glossary'
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'skill'
        response.result.description == 'Glossary terms'
        response.result.messages[0].content.text.contains('# Mauro Glossary')
        response.result.messages[0].content.text.contains('Use glossary terms.')
    }

    void 'prompts get missing name returns invalid params error'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 'missing-prompt',
            method : 'prompts/get',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 'missing-prompt'
        response.error.code == -32602
        response.error.message == 'prompts/get requires params.name'
    }

    void 'resources list without resource registry returns invalid params error'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 3,
            method : 'resources/list',
            params : [:]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 3
        response.error.code == -32602
        response.error.message == 'resources/list is not available'
    }

    void 'tools call missing name returns invalid params error'() {
        when:
        Map<String, Object> response = service.handle([
            jsonrpc: '2.0',
            id     : 4,
            method : 'tools/call',
            params : [
                arguments: [:]
            ]
        ] as Map<String, Object>) as Map<String, Object>

        then:
        response.jsonrpc == '2.0'
        response.id == 4
        response.error.code == -32602
        response.error.message == 'tools/call requires params.name'
    }

    void 'batch omits notification responses'() {
        when:
        List<Object> response = service.handle([
            [
                jsonrpc: '2.0',
                method : 'notifications/initialized'
            ],
            [
                jsonrpc: '2.0',
                id     : 5,
                method : 'tools/list',
                params : [:]
            ]
        ] as List<Object>) as List<Object>

        then:
        response.size() == 1
        (response[0] as Map<String, Object>).id == 5
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
            ),
            new ChatSkillDefinition(
                id: 'mauro-glossary',
                name: 'Mauro Glossary',
                description: 'Glossary terms',
                scope: 'GLOBAL',
                version: '1.0.0',
                type: 'SKILL',
                priority: 100,
                routing: new SkillRouting(
                    useWhen: ['users ask what a Mauro term means']
                ),
                toolApplicability: [
                    new SkillToolApplicability(
                        tool: 'mauro_keyword_search',
                        relationship: 'RECOMMENDED_CONTEXT',
                        useWhen: ['Mauro terminology is unclear'],
                        instructions: ['Use terms to choose domainTypes.']
                    )
                ],
                instruction: 'Use glossary terms.'
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
