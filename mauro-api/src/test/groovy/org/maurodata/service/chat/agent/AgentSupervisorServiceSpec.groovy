package org.maurodata.service.chat.agent

import org.maurodata.plugin.chat.api.chat.ChatEventDto
import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.SendMessageRequest
import org.maurodata.plugin.chat.api.chat.SessionDto
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.plugin.chat.api.chat.ToolSummaryDto
import org.maurodata.service.chat.ChatPromptAssetDefinition
import org.maurodata.service.chat.ChatPromptAssetService
import org.maurodata.service.chat.SkillToolApplicability
import org.maurodata.service.chat.ChatInMemoryStore
import org.maurodata.service.chat.ChatMcpService
import org.maurodata.service.chat.llm.LlmProvider
import org.maurodata.service.chat.llm.ProviderChunk
import org.maurodata.service.chat.llm.ProviderRegistry
import org.maurodata.service.chat.llm.ProviderRequest
import reactor.core.publisher.Flux
import spock.lang.Specification

import java.time.Instant

class AgentSupervisorServiceSpec extends Specification {

    void 'supervisor executes a tool call when the model stops after emitting it and still writes final answer'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        List<String> contextPrompts = []
        List<String> plannerPrompts = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        contextPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        plannerPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Compare two transplant catalogue items",
                              "fitness":"usable",
                              "successCriteria":["find the items","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search catalogue",
                                "objective":"Search for both named items",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "expectedOutput":"Search results for the two item names",
                                "successCriteria":["search result evidence exists"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-search-1',
                                name: 'mauro_search',
                                arguments: [searchTerm: '"Pre-Transplant Assessment" OR "Transplant Admission"']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Search evidence is available.",
                              "reason": "The step completed successfully.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Enough evidence is available for this focused test.",
                              "reason": "The goal can be answered.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final comparison answer.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(
                            name: 'mauro_search',
                            description: 'Search Mauro catalogue',
                            inputSchema: [type: 'object']
                        )
                    ]
                )
            ]
            invokeTool('mauro_search', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        output: [
                            count: 2,
                            items: [
                                [label: 'Pre-Transplant Assessment'],
                                [label: 'Transplant Admission']
                            ]
                        ]
                    ],
                    modelText: 'Tool mauro_search succeeded with 2 matching catalogue items.'
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            Stub(ChatPromptAssetService) {
                listAssetsByType('PERSONA') >> [
                    new ChatPromptAssetDefinition(id: 'mauro-persona', name: 'Mauro Persona', type: 'PERSONA', priority: 1, description: 'Persona', instruction: 'Always interpret Mauro catalogue tasks carefully.')
                ]
                listAssetsByType('SKILL') >> [
                    new ChatPromptAssetDefinition(id: 'mauro-data-model-explorer', name: 'Data model explorer', type: 'SKILL', priority: 10, description: 'Explore Mauro data models', instruction: 'Use search to find a model, then read its details.')
                ]
                searchAssets(_ as String) >> [
                    new ChatPromptAssetDefinition(id: 'mauro-data-model-explorer', name: 'Data model explorer', type: 'SKILL', priority: 10, description: 'Explore Mauro data models', instruction: 'Use search to find a model, then read its details.')
                ]
            },
            4,
            8,
            3
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare `Pre-Transplant Assessment` to `Transplant Admission`'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_final']
        events*.type.contains('agent_context_resolved')
        contextPrompts.first().contains('Persona guidance:')
        contextPrompts.first().contains('mauro-persona')
        contextPrompts.first().contains('Skill lookup result:')
        contextPrompts.first().contains('mauro-data-model-explorer')
        plannerPrompts.first().contains('Resolved planning context:')
        plannerPrompts.first().contains('Use typed URIs from tool results.')
        events*.type.contains('agent_plan_created')
        events.find {it.type == 'agent_status_changed' && it.metadata.requestedStatus == 'in_progress'}.metadata.validTransition == true
        events.find {it.type == 'agent_step_status_changed' && it.metadata.requestedStatus == 'completed'}.metadata.validTransition == true
        events.find {it.type == 'agent_plan_status_changed' && it.metadata.requestedStatus == 'complete'}.metadata.validTransition == true
        events*.type.contains('tool_call')
        events*.type.contains('tool_result')
        events*.type.contains('agent_evidence_added')
        events*.type.contains('agent_run_completed')
        events.find {it.type == 'agent_run_completed'}.metadata.status == 'completed'
        events.findAll {it.type.startsWith('agent_operation_')}*.metadata*.roleName.containsAll([
            'context_resolver',
            'planner',
            'executor',
            'tool_call',
            'step_evaluator',
            'plan_evaluator',
            'final_writer'
        ])
        events.findAll {it.type == 'agent_operation_completed'}*.metadata*.validTransition.every {it == true}
        events.find {it.type == 'tool_result'}.metadata.evidenceId
        events.find {it.type == 'token'}.content == 'Final comparison answer.'
        store.agentRuns.values().first().status == 'completed'
        store.agentPlans.size() == 1
        store.agentContexts.size() == 1
        events.find {it.type == 'agent_context_resolved'}.metadata.personaSkills.first().id == 'mauro-persona'
        events.find {it.type == 'agent_context_resolved'}.metadata.skillLookupResults.first().id == 'mauro-data-model-explorer'
        events.find {it.type == 'agent_final_context'}.content.contains('Always interpret Mauro catalogue tasks carefully.')
        store.agentEvidence.size() == 1
    }

    void 'supervisor skips guarded fallback step when final evidence already exists'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        int toolInvocations = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "fitness":"high",
                              "successCriteria":["Evidence: A useful list of diabetes form matches is retrieved."],
                              "assumptions":[],
                              "risks":[],
                              "steps":[
                                {
                                  "title":"Search for diabetes forms",
                                  "objective":"Find DataModel catalogue items matching diabetes.",
                                  "kind":"search",
                                  "allowedTools":["mauro_search"],
                                  "guard":"always",
                                  "guardReason":"Primary search is required.",
                                  "optional":false,
                                  "expectedOutput":"Search results for diabetes forms.",
                                  "successCriteria":["Evidence: Search results exist."]
                                },
                                {
                                  "title":"Fallback keyword search",
                                  "objective":"Try a keyword-only search if the primary search produced no final evidence.",
                                  "kind":"search",
                                  "allowedTools":["mauro_keyword_search"],
                                  "guard":"if_no_final_evidence",
                                  "guardReason":"Only needed when the primary search did not produce answerable evidence.",
                                  "optional":true,
                                  "expectedOutput":"Fallback search results.",
                                  "successCriteria":["Evidence: Fallback search results exist."]
                                }
                              ]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-search-1',
                                name: 'mauro_search',
                                arguments: [searchTerm: 'diabetes', domainTypes: ['DataModel']]
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Search results were retrieved.",
                              "reason": "The primary search returned usable results.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "continue",
                              "summary": "A fallback step remains but is only needed if no final evidence exists.",
                              "reason": "Continue to the next planned step if its guard allows it.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Found diabetes forms.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                        new ToolSummaryDto(name: 'mauro_keyword_search', description: 'Keyword search Mauro catalogue', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                toolInvocations++
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        output: [
                            count: 1,
                            items: [[label: 'Adult Diabetes Education Form', domainType: 'DataModel']]
                        ]
                    ],
                    modelText: 'Use a table and mention that more paging may be available.'
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-guard', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-guard',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        toolInvocations == 1
        providerPurposes.count {it == 'agent_executor'} == 1
        events*.type.contains('agent_step_skipped')
        events.find {it.type == 'agent_step_skipped'}.metadata.guard == 'if_no_final_evidence'
        events.find {it.type == 'agent_step_status_changed' && it.metadata.requestedStatus == 'skipped'}.metadata.validTransition == true
        events*.type.contains('agent_run_completed')
        store.agentSteps.values().count {it.status == 'skipped'} == 1
        store.agentSteps.values().find {it.title == 'Fallback keyword search'}.optional
        events.find {it.type == 'token'}.content == 'Found diabetes forms.'
    }

    void 'required dependent step is not skipped when model gives it a fallback guard'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> invokedTools = []
        int executorAttempts = 0
        int planEvaluations = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Look more closely at a specific form",
                              "fitness":"high",
                              "successCriteria":["Evidence: resource details are retrieved"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[
                                {
                                  "title":"Identify form",
                                  "objective":"Search for the named form.",
                                  "kind":"search",
                                  "allowedTools":["mauro_search"],
                                  "guard":"always",
                                  "guardReason":"Need to identify the item.",
                                  "optional":false,
                                  "expectedOutput":"Search result.",
                                  "successCriteria":["Evidence: matching item id exists"]
                                },
                                {
                                  "title":"Retrieve form details",
                                  "objective":"Use mauro_get with the identified item to retrieve the content of the form.",
                                  "kind":"get",
                                  "allowedTools":["mauro_get"],
                                  "guard":"if_no_successful_tool_evidence",
                                  "guardReason":"Model incorrectly marked this required dependent step as conditional.",
                                  "optional":false,
                                  "expectedOutput":"Resource details.",
                                  "successCriteria":["Evidence: form details exist"]
                                }
                              ]
                            }''')
                        ])
                    case 'agent_executor':
                        executorAttempts++
                        if (executorAttempts == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-search',
                                    name: 'mauro_search',
                                    arguments: [searchTerm: 'Adult Diabetes Eye Assessment Form']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/form-id']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "The step completed.",
                              "reason": "Tool evidence exists.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        planEvaluations++
                        return Flux.fromIterable([
                            token(request, """{
                              "decision": "${planEvaluations == 1 ? 'continue' : 'final'}",
                              "summary": "Plan progress is acceptable.",
                              "reason": "Continue until details are retrieved.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }""")
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Form details answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                        new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                invokedTools.add(toolName)
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        arguments: request.arguments,
                        output: [id: 'form-id', label: 'Adult Diabetes Eye Assessment Form']
                    ],
                    modelText: "Tool ${toolName} succeeded.".toString()
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-required-guard', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Look more closely at Adult Diabetes Eye Assessment Form'),
            'assistant-required-guard',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        invokedTools == ['mauro_search', 'mauro_get']
        !events*.type.contains('agent_step_skipped')
        AgentStepRecord getStep = store.agentSteps.values().find {it.title == 'Retrieve form details'}
        getStep.guard == 'always'
        getStep.metadata.rawGuard == 'if_no_successful_tool_evidence'
        getStep.metadata.guardNormalizedForRequiredStep == true
        events.find {it.type == 'token'}.content == 'Form details answer.'
    }

    void 'planner no-step output retries through operation state machine before succeeding'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        int plannerAttempts = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        plannerAttempts++
                        if (plannerAttempts == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "goalRestatement":"Find forms about diabetes",
                                  "fitness":"draft",
                                  "successCriteria":["Evidence: Find matching forms."],
                                  "assumptions":[],
                                  "risks":[],
                                  "steps":[]
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "fitness":"usable",
                              "successCriteria":["Evidence: Find matching forms."],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search for diabetes forms",
                                "objective":"Find DataModel catalogue items matching diabetes.",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "guard":"always",
                                "guardReason":"A search is needed.",
                                "optional":false,
                                "expectedOutput":"Search results.",
                                "successCriteria":["Evidence: Search results exist."]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-search-retry',
                                name: 'mauro_search',
                                arguments: [searchTerm: 'diabetes']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Search completed.",
                              "reason": "The step produced evidence.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "The useful search evidence is sufficient.",
                              "reason": "The goal can be answered with caveats.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Found matching forms.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object'])]
                )
            ]
            invokeTool('mauro_search', _ as ToolInvokeRequest) >> new ToolInvokeResponse(
                success: true,
                result: [tool: 'mauro_search', output: [count: 1, items: [[label: 'Adult Diabetes Education Form']]]],
                modelText: 'Search completed.'
            )
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-planner-retry', workspaceId: 'default', model: 'tiny-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-planner-retry',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        plannerAttempts == 2
        events.find {it.type == 'agent_operation_failed' && it.metadata.roleName == 'planner'}
        events.find {it.type == 'agent_operation_retrying' && it.metadata.roleName == 'planner'}.metadata.validTransition == true
        events.findAll {it.type == 'agent_operation_in_progress' && it.metadata.roleName == 'planner'}*.metadata*.attempt == [1, 2]
        events.find {it.type == 'agent_operation_completed' && it.metadata.roleName == 'planner'}.metadata.attempt == 2
        !events*.type.contains('agent_run_failed')
        events*.type.contains('agent_run_completed')
        events.find {it.type == 'token'}.content == 'Found matching forms.'
    }

    void 'supervisor does not write final answer when evaluator reports incomplete step with continue decision'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Compare two catalogue items",
                              "fitness":"usable",
                              "successCriteria":["read both items","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Compare findings",
                                "objective":"Compare the two requested items",
                                "kind":"compare",
                                "allowedTools":[],
                                "expectedOutput":"A comparison",
                                "successCriteria":["both items have been retrieved"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            token(request, 'I only have details for Pre-Transplant Assessment.')
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": false,
                              "decision": "replan",
                              "summary": "Transplant Admission details are still missing.",
                              "reason": "The comparison cannot be completed until the second item is retrieved.",
                              "question": null
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'This final answer should not be emitted.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            Stub(ChatMcpService) { listServers() >> [] },
            4,
            8,
            0
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare `Pre-Transplant Assessment` to `Transplant Admission`'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator']
        events*.type.contains('agent_step_failed')
        events*.type.contains('agent_run_failed')
        !events*.type.contains('agent_final_started')
        events.findAll {it.type == 'token'}*.content.join('') == ''
        events*.type.contains('agent_evidence_added')
        store.agentRuns.values().first().status == 'failed'
    }

    void 'supervisor replans after incomplete continue decision and executes revised plan'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        int plannerCalls = 0
        int evaluatorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        plannerCalls++
                        if (plannerCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "goalRestatement":"Compare two catalogue items",
                                  "fitness":"usable",
                                  "successCriteria":["read both items","compare them"],
                                  "assumptions":[],
                                  "risks":[],
                                  "steps":[{
                                    "title":"Search for both items",
                                    "objective":"Find catalogue candidates for the named items",
                                    "kind":"search",
                                    "allowedTools":["mauro_search"],
                                    "expectedOutput":"Search result evidence",
                                    "successCriteria":["candidate ids are known"]
                                  }]
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Continue comparison after search evidence",
                              "fitness":"usable",
                              "successCriteria":["read Transplant Admission","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read Transplant Admission",
                                "objective":"Retrieve the full details for Transplant Admission",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Transplant Admission details",
                                "successCriteria":["Transplant Admission detail evidence exists"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        if (plannerCalls == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-search-1',
                                    name: 'mauro_search',
                                    arguments: [searchTerm: 'Pre-Transplant Assessment OR Transplant Admission']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-1',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/transplant-admission']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        evaluatorCalls++
                        if (evaluatorCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "stepComplete": false,
                              "decision": "replan",
                                  "summary": "Search found candidates, but details are missing.",
                                  "reason": "Use the search evidence to retrieve the Transplant Admission details.",
                                  "question": null
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Required details are available.",
                              "reason": "The step completed successfully.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Required details are available.",
                              "reason": "The evidence is sufficient for final synthesis.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final comparison after replan.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                        new ToolSummaryDto(name: 'mauro_get', description: 'Read a Mauro resource', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                new ToolInvokeResponse(
                    success: true,
                    result: [tool: toolName, output: [ok: true, arguments: request.arguments]],
                    modelText: "Tool ${toolName} succeeded."
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare `Pre-Transplant Assessment` to `Transplant Admission`'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_final']
        events*.type.contains('agent_replan_started')
        events*.type.contains('agent_replan_completed')
        events*.type.contains('agent_run_completed')
        events.findAll {it.type == 'tool_call'}*.metadata*.name == ['mauro_search', 'mauro_get']
        events.find {it.type == 'token' && it.content == 'Final comparison after replan.'}
        store.agentRuns.values().first().status == 'completed'
    }

    void 'supervisor advances to next planned step when current step completed but goal still needs more work'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        int executorCalls = 0
        int evaluatorCalls = 0
        int planEvaluatorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Compare two catalogue items",
                              "fitness":"usable",
                              "successCriteria":["read both items","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read Pre-Transplant Assessment",
                                "objective":"Retrieve the full details for Pre-Transplant Assessment",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Pre-Transplant Assessment details",
                                "successCriteria":["Pre-Transplant Assessment detail evidence exists"]
                              },{
                                "title":"Read Transplant Admission",
                                "objective":"Retrieve the full details for Transplant Admission",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Transplant Admission details",
                                "successCriteria":["Transplant Admission detail evidence exists"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        executorCalls++
                        if (executorCalls == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-get-pre',
                                    name: 'mauro_get',
                                    arguments: [uri: 'mauro-api://http-get/api/dataModels/pre-transplant-assessment']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-admission',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/transplant-admission']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        evaluatorCalls++
                        if (evaluatorCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "stepComplete": true,
                                  "decision": "continue",
                                  "summary": "Pre-Transplant Assessment details were retrieved.",
                                  "reason": "This step is complete; continue to the planned Transplant Admission read step.",
                                  "question": null
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Both details are available.",
                              "reason": "The step completed successfully.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        planEvaluatorCalls++
                        if (planEvaluatorCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "decision": "continue",
                                  "summary": "The first read is complete and the second planned read remains valid.",
                                  "reason": "Continue to the Transplant Admission read step.",
                                  "question": null,
                                  "missing": ["Transplant Admission details"],
                                  "obsoleteStepIds": []
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Both details are available.",
                              "reason": "The evidence is sufficient for final synthesis.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final comparison after planned reads.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_get', description: 'Read a Mauro resource', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool('mauro_get', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                new ToolInvokeResponse(
                    success: true,
                    result: [tool: toolName, output: [ok: true, arguments: request.arguments]],
                    modelText: "Tool ${toolName} succeeded for ${request.arguments.uri}."
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare `Pre-Transplant Assessment` to `Transplant Admission`'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_final']
        !events*.type.contains('agent_replan_started')
        events.findAll {it.type == 'agent_plan_evaluated'}*.metadata*.decision == ['continue', 'final']
        events.findAll {it.type == 'tool_call'}*.metadata*.arguments*.uri == [
            'mauro-api://http-get/api/dataModels/pre-transplant-assessment',
            'mauro-api://http-get/api/dataModels/transplant-admission'
        ]
        events.findAll {it.type == 'agent_step_completed'}.size() == 2
        events.find {it.type == 'token' && it.content == 'Final comparison after planned reads.'}
        store.agentRuns.values().first().status == 'completed'
    }

    void 'supervisor treats step evaluator continue decision as completed even if model reports stepComplete false'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find one item",
                              "fitness":"usable",
                              "successCriteria":["search"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search item",
                                "objective":"Search for the item",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "expectedOutput":"Search results",
                                "successCriteria":["search results exist"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-search-1',
                                name: 'mauro_search',
                                arguments: [searchTerm: 'Pre-Transplant Assessment']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": false,
                              "decision": "continue",
                              "summary": "The search executed successfully.",
                              "reason": "Continue to the next run-level decision.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Search evidence is enough for this focused test.",
                              "reason": "The goal can be answered.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final answer after normalized step.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object'])
                ])
            ]
            invokeTool('mauro_search', _ as ToolInvokeRequest) >> new ToolInvokeResponse(success: true, result: [tool: 'mauro_search', output: [ok: true]], modelText: 'Search succeeded.')
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find item'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_final']
        events*.type.contains('agent_step_completed')
        !events*.type.contains('agent_step_failed')
        events.find {it.type == 'token' && it.content == 'Final answer after normalized step.'}
    }

    void 'final context renders tool result evidence rather than tool guidance text'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                switch (request.options?.purpose as String) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Read one catalogue item",
                              "fitness":"usable",
                              "successCriteria":["Evidence: actual tool result is available"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read item",
                                "objective":"Read one item",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Item details",
                                "successCriteria":["Evidence: item details are returned"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-1',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/example']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "The item was read.",
                              "reason": "The step completed.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "The evidence is available.",
                              "reason": "The goal can be answered.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final answer.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_get', description: 'Read a Mauro resource', inputSchema: [type: 'object'])
                ])
            ]
            invokeTool('mauro_get', _ as ToolInvokeRequest) >> new ToolInvokeResponse(
                success: true,
                result: [tool: 'mauro_get', output: [label: 'Actual Tool Payload', id: 'item-1']],
                modelText: 'GUIDANCE ONLY: do not use as evidence'
            )
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Read item'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        ChatEventDto finalContext = events.find {it.type == 'agent_final_context'}
        finalContext.content.contains('Actual Tool Payload')
        finalContext.content.contains('Final-answer tool guidance to follow:')
        finalContext.content.contains('GUIDANCE ONLY')
        !finalContext.content.substring(finalContext.content.indexOf('Evidence:'), finalContext.content.indexOf('Final-answer tool guidance to follow:')).contains('GUIDANCE ONLY')
        AgentEvidenceRecord evidence = store.agentEvidence.values().first()
        AgentGuidanceRecord guidance = store.agentGuidance.values().first()
        !evidence.metadata.containsKey('modelText')
        evidence.metadata.guidanceId == guidance.id
        guidance.metadata.evidenceId == evidence.id
        guidance.content == 'GUIDANCE ONLY: do not use as evidence'
    }

    void 'successful search evidence is rendered without supervisor intent guessing'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> executorPrompts = []
        List<String> planEvaluatorPrompts = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                switch (request.options?.purpose as String) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"List diabetes forms",
                              "fitness":"usable",
                              "successCriteria":["Answer: matching search results are listed"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search for DataModels related to diabetes",
                                "objective":"Search catalogue DataModel items matching diabetes",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "expectedOutput":"Search results",
                                "successCriteria":["Output: search results are returned"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        executorPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([
                            new ProviderChunk('tool_result', request.messageId, null, [
                                callId: 'call-search-forms',
                                ok: true,
                                output: [tool: 'mauro_search', output: [count: 6, offset: 0, max: 5, hasMore: true, items: [[label: 'Diabetes Form', domainType: 'DataModel', id: 'dm-1']]]]
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Matching forms were found.",
                              "reason": "The listing step completed.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        planEvaluatorPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "replan",
                              "summary": "The first page returned matching forms but hasMore is true.",
                              "reason": "The search returned 6 total results and only 5 were shown on the first page. Since the user asked to find forms, fetch the next page.",
                              "question": null,
                              "missing": ["next page"],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Listed forms.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object'])
                ])
            ]
            renderModelText('mauro_search', _ as Map<String, Object>) >> ('x' * 1700) + '\n## Answer Instructions\nCOMMON: Tell the user the exact search term used: diabetes.\nCOMMON: Use this pagination summary in your answer: Page 1 of 2. Showing 1-5 of 6 matching catalogue items, 5 results at a time.'
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, formSkillService(), 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        AgentEvidenceRecord evidence = store.agentEvidence.values().first()
        evidence.metadata.evidenceRole == 'tool_result'
        evidence.metadata.pertinentToFinal == true
        store.agentGuidance.values().first().followForFinal == true
        executorPrompts.first().contains('mauro-form-representation')
        executorPrompts.first().contains('"forms about X"')
        executorPrompts.first().contains('mauro_search domainTypes ["DataModel"]')
        planEvaluatorPrompts.first().contains('mauro-form-representation')
        planEvaluatorPrompts.first().contains('mauro_search domainTypes ["DataModel"]')
        ChatEventDto finalContext = events.find {it.type == 'agent_final_context'}
        finalContext.content.contains('Diabetes Form')
        finalContext.content.contains('## Answer Instructions')
        finalContext.content.contains('COMMON: Tell the user the exact search term used: diabetes.')
        finalContext.content.contains('COMMON: Use this pagination summary in your answer: Page 1 of 2. Showing 1-5 of 6 matching catalogue items, 5 results at a time.')
        !finalContext.content.contains('No final-answer tool guidance.')
        events.find {it.type == 'agent_plan_evaluated'}.metadata.decision == 'final'
        events.find {it.type == 'agent_plan_evaluated'}.metadata.reason.contains('did not identify an unmet')
        !events*.type.contains('agent_replan_started')
        events*.type.contains('agent_guidance_added')
    }

    void 'final context includes successful guidance for rendered tool evidence'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                switch (request.options?.purpose as String) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find one candidate before reading details",
                              "fitness":"usable",
                              "successCriteria":["Evidence: candidate search result is available"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search for a specific resource id",
                                "objective":"Identify resource id before retrieving details",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "expectedOutput":"Candidate resource id",
                                "successCriteria":["Evidence: resource id is returned"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_result', request.messageId, null, [
                                callId: 'call-search-intermediate',
                                ok: true,
                                output: [tool: 'mauro_search', output: [count: 1, items: [[label: 'Candidate Item', domainType: 'DataModel', id: 'dm-1']]]]
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Candidate was found.",
                              "reason": "The search completed.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Use the available search evidence.",
                              "reason": "The available evidence is enough for this test.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Final answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object'])
                ])
            ]
            renderModelText('mauro_search', _ as Map<String, Object>) >> '## Answer Instructions\nCOMMON: Show the search results in a table.'
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find candidate'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        AgentEvidenceRecord evidence = store.agentEvidence.values().first()
        AgentGuidanceRecord guidance = store.agentGuidance.values().first()
        evidence.metadata.evidenceRole == 'tool_result'
        evidence.metadata.pertinentToFinal == true
        guidance.followForFinal == true
        evidence.metadata.guidanceId == guidance.id
        ChatEventDto finalContext = events.find {it.type == 'agent_final_context'}
        finalContext.content.contains('Candidate Item')
        finalContext.content.contains('## Answer Instructions')
        finalContext.content.contains('COMMON: Show the search results in a table.')
        !finalContext.content.contains('No final-answer tool guidance.')
    }

    void 'successful search and read evidence are both renderable without supervisor intent guessing'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        int executorCalls = 0
        int planEvaluatorCalls = 0
        List<String> executorPrompts = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                switch (request.options?.purpose as String) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Read item after search",
                              "fitness":"usable",
                              "successCriteria":["Evidence: item details are read"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search for item candidate",
                                "objective":"Find the specific resource ID before reading details",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "expectedOutput":"Candidate resource ID",
                                "successCriteria":["Tool: candidate ID is known"]
                              },{
                                "title":"Read item details",
                                "objective":"Retrieve details using the candidate resource ID",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Full details",
                                "successCriteria":["Evidence: item details are available"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        executorPrompts.add(request.messages.last().content)
                        executorCalls++
                        if (executorCalls == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-search-item',
                                    name: 'mauro_search',
                                    arguments: [searchTerm: 'Example']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-item',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/example']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Step completed.",
                              "reason": "The step succeeded.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        planEvaluatorCalls++
                        if (planEvaluatorCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "decision": "continue",
                                  "summary": "Read step remains.",
                                  "reason": "Search identified a candidate only.",
                                  "question": null,
                                  "missing": ["item details"],
                                  "obsoleteStepIds": []
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Read evidence is available.",
                              "reason": "The goal can be answered.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Read item.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                    new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])
                ])
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                new ToolInvokeResponse(
                    success: true,
                    result: [tool: toolName, output: [label: toolName == 'mauro_get' ? 'Full Item Detail' : 'Search Candidate']],
                    modelText: "Tool ${toolName} succeeded."
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Read item details'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        List<AgentEvidenceRecord> evidence = store.agentEvidence.values() as List<AgentEvidenceRecord>
        evidence.find {it.sourceName == 'mauro_search'}.metadata.evidenceRole == 'tool_result'
        evidence.find {it.sourceName == 'mauro_search'}.metadata.pertinentToFinal == true
        evidence.find {it.sourceName == 'mauro_get'}.metadata.evidenceRole == 'tool_result'
        evidence.find {it.sourceName == 'mauro_get'}.metadata.pertinentToFinal == true
        executorPrompts.size() == 2
        executorPrompts.last().contains('Prior tool guidance:')
        executorPrompts.last().contains('Tool mauro_search succeeded.')
        ChatEventDto finalContext = events.find {it.type == 'agent_final_context'}
        finalContext.content.contains('Search Candidate')
        finalContext.content.contains('Full Item Detail')
        !finalContext.content.contains('Omitted Intermediate Evidence')
    }

    void 'final context includes guidance linked to rendered failed read evidence'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                switch (request.options?.purpose as String) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Read item",
                              "fitness":"usable",
                              "successCriteria":["Evidence: read attempted"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read item",
                                "objective":"Retrieve details for the item",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Item details",
                                "successCriteria":["Tool: read attempted"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-bad',
                                name: 'mauro_get',
                                arguments: [uri: '019ddee8-a68d-7fc9-b84b-017d9e687e2c']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "The read failed but generated recovery guidance.",
                              "reason": "This focused test can finish.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Finish focused test.",
                              "reason": "The failure is represented.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Failure reported.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(id: 'local-mcp', name: 'Local MCP', tools: [
                    new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])
                ])
            ]
            invokeTool('mauro_get', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                throw new IllegalArgumentException("Unknown resource URI: ${request.arguments.uri}".toString())
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(store, new ProviderRegistry([provider]), mcpService, 4, 8, 2)
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Read item'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        AgentEvidenceRecord evidence = store.agentEvidence.values().first()
        AgentGuidanceRecord guidance = store.agentGuidance.values().first()
        guidance.followForFinal == false
        evidence.metadata.guidanceId == guidance.id
        guidance.metadata.evidenceId == evidence.id
        ChatEventDto finalContext = events.find {it.type == 'agent_final_context'}
        finalContext.content.contains('Tool mauro_get failed')
        finalContext.content.contains('Tool mauro_get was not executed')
        !finalContext.content.contains('No final-answer tool guidance.')
    }

    void 'supervisor repairs malformed planner JSON during replan and continues execution'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        int plannerCalls = 0
        int evaluatorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        plannerCalls++
                        if (plannerCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "goalRestatement":"Compare two catalogue items",
                                  "fitness":"usable",
                                  "successCriteria":["read both items","compare them"],
                                  "assumptions":[],
                                  "risks":[],
                                  "steps":[{
                                    "title":"Search for candidates",
                                    "objective":"Find candidate items",
                                    "kind":"search",
                                    "allowedTools":["mauro_search"],
                                    "expectedOutput":"Candidate ids",
                                    "successCriteria":["candidate ids are known"]
                                  }]
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Retrieve \\`Transplant Admission\\` details",
                              "fitness":"usable",
                              "successCriteria":["read Transplant Admission","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read Transplant Admission",
                                "objective":"Retrieve the full details for Transplant Admission",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Transplant Admission details",
                                "successCriteria":["Transplant Admission detail evidence exists"]
                              }]
                            ]''')
                        ])
                    case 'agent_planner_json_repair':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Retrieve the missing Transplant Admission details",
                              "fitness":"usable",
                              "successCriteria":["read Transplant Admission","compare them"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Read Transplant Admission",
                                "objective":"Retrieve the full details for Transplant Admission",
                                "kind":"read",
                                "allowedTools":["mauro_get"],
                                "expectedOutput":"Transplant Admission details",
                                "successCriteria":["Transplant Admission detail evidence exists"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        if (plannerCalls == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-search-1',
                                    name: 'mauro_search',
                                    arguments: [searchTerm: 'Pre-Transplant Assessment OR Transplant Admission']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get-1',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/transplant-admission']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        evaluatorCalls++
                        if (evaluatorCalls == 1) {
                            return Flux.fromIterable([
                                token(request, '''{
                                  "stepComplete": false,
                              "decision": "replan",
                                  "summary": "Search found candidates, but details are missing.",
                                  "reason": "Retrieve Transplant Admission before final comparison.",
                                  "question": null
                                }''')
                            ])
                        }
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Required details are available.",
                              "reason": "The step completed successfully.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "Required details are available.",
                              "reason": "The evidence is sufficient for final synthesis.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([
                            token(request, 'Final comparison after repaired replan.')
                        ])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                        new ToolSummaryDto(name: 'mauro_get', description: 'Read a Mauro resource', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest request ->
                new ToolInvokeResponse(
                    success: true,
                    result: [tool: toolName, output: [ok: true, arguments: request.arguments]],
                    modelText: "Tool ${toolName} succeeded."
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare `Pre-Transplant Assessment` to `Transplant Admission`'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes == ['agent_context_resolver', 'agent_planner', 'agent_executor', 'agent_step_evaluator', 'agent_context_resolver', 'agent_planner', 'agent_planner_json_repair', 'agent_executor', 'agent_step_evaluator', 'agent_plan_evaluator', 'agent_final']
        events*.type.contains('agent_replan_started')
        events*.type.contains('agent_replan_completed')
        events*.type.contains('agent_run_completed')
        !events*.type.contains('agent_run_failed')
        events.find {it.type == 'token' && it.content == 'Final comparison after repaired replan.'}
        store.agentRuns.values().first().status == 'completed'
    }

    void 'supervisor clean retries strict JSON role after repair failure'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> providerPurposes = []
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                providerPurposes.add(purpose)
                switch (purpose) {
                    case 'agent_context_resolver':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "domainContext":"Find form resources",
                              "relevantTools":["mauro_search"],
                              "recommendedSkills":["mauro-form-representation"],
                              "relevantResources":[],
                              "planningHints":"Use domainTypes ["DataModel"]",
                              "constraints":"Ground the answer"
                            }''')
                        ])
                    case 'agent_context_resolver_json_repair':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "domainContext":["Find form resources"],
                              "relevantTools":[{"name":"mauro_search","reason":"search","readOnly":true}],
                              "recommendedSkills":[{"id":"mauro-form-representation","reason":"forms","usage":"planning"}],
                              "relevantResources":[],
                              "planningHints":["Use domainTypes''')
                        ])
                    case 'agent_context_resolver_json_retry':
                        return Flux.fromIterable([token(request, contextJson())])
                    case 'agent_planner':
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "fitness":"usable",
                              "successCriteria":["Answer: a useful result is provided"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Summarise result",
                                "objective":"Provide a useful result from available context",
                                "kind":"analysis",
                                "allowedTools":[],
                                "expectedOutput":"Summary",
                                "successCriteria":["Output: summary is available"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        return Flux.fromIterable([token(request, 'Found forms summary.')])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "stepComplete": true,
                              "decision": "continue",
                              "summary": "Summary is available.",
                              "reason": "The step completed.",
                              "question": null
                            }''')
                        ])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([
                            token(request, '''{
                              "decision": "final",
                              "summary": "The answer can be written.",
                              "reason": "The plan is complete.",
                              "question": null,
                              "missing": [],
                              "obsoleteStepIds": []
                            }''')
                        ])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Final after clean retry.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            Stub(ChatMcpService) { listServers() >> [] },
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-1', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-1',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        providerPurposes.contains('agent_context_resolver_json_repair')
        providerPurposes.contains('agent_context_resolver_json_retry')
        events.find {it.type == 'token' && it.content == 'Final after clean retry.'}
        !events*.type.contains('agent_run_failed')
        store.agentRuns.values().first().status == 'completed'
    }

    void 'new agent turn receives prior session continuity in context resolver and planner context'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> contextPrompts = []
        List<String> plannerPrompts = []
        List<String> finalPrompts = []
        List<Map<String, Object>> invokedArguments = []
        int executorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        String prompt = request.messages.last().content
                        contextPrompts.add(prompt)
                        def evidenceMatcher = prompt =~ /evidenceId=([^\s]+)/
                        def guidanceMatcher = prompt =~ /guidanceId=([^\s]+)/
                        String priorEvidenceId = evidenceMatcher.find() ? evidenceMatcher.group(1) : ''
                        String priorGuidanceId = guidanceMatcher.find() ? guidanceMatcher.group(1) : ''
                        return Flux.fromIterable([token(request, """{
                          "goalRestatement": "Resolve follow-up context",
                          "followUpInterpretation": "${priorEvidenceId ? 'paging_request' : 'new_task'}",
                          "goalFrame": {
                            "userGoal": "show more",
                            "interpretedGoal": "Continue the previous search.",
                            "scope": "Retrieve the next available page when previous paging state is available.",
                            "nonGoals": ["Do not start an unrelated search."],
                            "acceptableCompletion": {
                              "mode": "answer_with_caveats",
                              "minimumEvidence": ["One page of continuation results is available."],
                              "acceptableCaveats": ["More pages may exist."],
                              "mustNotBlockOn": ["Additional pages existing."]
                            }
                          },
                          "domainContext": ["Mauro catalogue follow-up"],
                          "relevantTools": [{"name": "mauro_search", "reason": "Continue previous search", "readOnly": true}],
                          "recommendedSkills": [],
                          "relevantResources": [],
                          "instructions": [{"type": "tool_call_suggestion", "target": "planner", "instruction": "Continue the previous mauro_search using prior paging state."}],
                          "resolvedReferences": [],
                          "priorEvidenceToReuse": ${priorEvidenceId ? """[{"evidenceId": "${priorEvidenceId}", "reason": "Previous search evidence defines the paging context."}]""" : '[]'},
                          "priorGuidanceToFollow": ${priorGuidanceId ? """[{"guidanceId": "${priorGuidanceId}", "reason": "Previous search guidance explains how to render paging."}]""" : '[]'},
                          "planningHints": ["Use prior search context when available."],
                          "constraints": ["Do not broaden the goal."]
                        }""")])
                    case 'agent_planner':
                        plannerPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([
                            token(request, '''{
                              "goalRestatement":"Search diabetes forms",
                              "fitness":"usable",
                              "successCriteria":["Evidence: useful search evidence exists"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search",
                                "objective":"Run the relevant search.",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "guard":"always",
                                "guardReason":"Primary search step.",
                                "optional":false,
                                "expectedOutput":"Search results.",
                                "successCriteria":["Evidence: search results exist"]
                              }]
                            }''')
                        ])
                    case 'agent_executor':
                        executorCalls++
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: "call-${executorCalls}".toString(),
                                name: 'mauro_search',
                                arguments: [query: 'forms about diabetes']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "stepComplete": true,
                          "decision": "continue",
                          "summary": "Search evidence exists.",
                          "reason": "The tool returned results.",
                          "question": null
                        }''')])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "decision": "final",
                          "summary": "The search result can answer within scope.",
                          "reason": "Useful evidence is available and caveats are acceptable.",
                          "question": null,
                          "missing": [],
                          "obsoleteStepIds": []
                        }''')])
                    case 'agent_final':
                        finalPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([token(request, 'Search answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool('mauro_search', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest toolRequest ->
                invokedArguments.add(new LinkedHashMap<String, Object>(toolRequest.arguments ?: [:] as Map<String, Object>))
                Integer offset = (toolRequest.arguments?.offset ?: 0) as Integer
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        arguments: toolRequest.arguments,
                        output: [
                            count: 6,
                            hasMore: offset == 0,
                            nextOffset: 5,
                            items: [[label: offset == 0 ? 'Adult Diabetes Education Form' : 'Diabetes Follow-up Form', id: 'item-id']]
                        ]
                    ],
                    modelText: 'COMMON: Use this pagination summary in your answer: Page 1 of 2. More results are available.'
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            Stub(ChatPromptAssetService) {
                listAssetsByType('PERSONA') >> []
                listAssetsByType('SKILL') >> []
                searchAssets(_ as String) >> []
            },
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-continuity', workspaceId: 'default', model: 'fake-model')

        when:
        Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-first',
            store.messagesForSession(session.id),
            null
        )).collectList().block()
        List<ChatEventDto> secondEvents = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'show more'),
            'assistant-second',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        contextPrompts.size() == 2
        !contextPrompts.first().contains('Prior session continuity context:')
        contextPrompts.last().contains('Prior session continuity context:')
        contextPrompts.last().contains('Find forms about diabetes')
        contextPrompts.last().contains('Adult Diabetes Education Form')
        contextPrompts.last().contains('guidanceId=')
        plannerPrompts.last().contains('Goal frame:')
        plannerPrompts.last().contains('Scoped instructions:')
        plannerPrompts.last().contains('Session continuity available to Perceive:')
        plannerPrompts.last().contains('Evidence already gathered:')
        plannerPrompts.last().contains('Adult Diabetes Education Form')
        invokedArguments.size() == 2
        invokedArguments.first().offset == null
        invokedArguments.last().offset == 5
        invokedArguments.last().query == 'forms about diabetes'
        finalPrompts.last().contains('Final-answer tool guidance to follow:')
        finalPrompts.last().contains('COMMON: Use this pagination summary')
        secondEvents*.type.contains('agent_context_continuity')
        secondEvents.find {it.type == 'agent_context_resolved'}.metadata.followUpInterpretation == 'paging_request'
        secondEvents.find {it.type == 'agent_context_resolved'}.metadata.goalFrame.acceptableCompletion.mode == 'answer_with_caveats'
    }

    void 'perceiver can resolve a follow-up details request from prior resource references'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<String> plannerPrompts = []
        List<String> finalPrompts = []
        List<String> invokedTools = []
        int contextCalls = 0
        int plannerCalls = 0
        int executorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        contextCalls++
                        if (contextCalls == 1) {
                            return Flux.fromIterable([token(request, contextJson())])
                        }
                        String prompt = request.messages.last().content
                        def evidenceMatcher = prompt =~ /evidenceId=([^\s]+)/
                        String priorEvidenceId = evidenceMatcher.find() ? evidenceMatcher.group(1) : ''
                        return Flux.fromIterable([token(request, """{
                          "goalRestatement": "Look more closely at the Adult Diabetes Eye Assessment Form.",
                          "followUpInterpretation": "details_request",
                          "goalFrame": {
                            "userGoal": "Thanks. Let's look more closely at the Adult Diabetes Eye Assessment Form",
                            "interpretedGoal": "Retrieve details for the previously found Adult Diabetes Eye Assessment Form.",
                            "scope": "Details of the selected catalogue form.",
                            "nonGoals": ["Do not search again merely to identify the same form."],
                            "acceptableCompletion": {
                              "mode": "answer_with_caveats",
                              "minimumEvidence": ["The selected form resource has been read."],
                              "acceptableCaveats": ["Field-level detail may require further exploration."],
                              "mustNotBlockOn": ["Additional related forms."]
                            }
                          },
                          "domainContext": ["The requested form was already found in prior search results."],
                          "relevantTools": [{"name": "mauro_get", "reason": "Read the selected resource", "readOnly": true}],
                          "recommendedSkills": [],
                          "relevantResources": [],
                          "instructions": [{"type": "resolved_resource", "target": "planner", "instruction": "Use the resolved Adult Diabetes Eye Assessment Form resource from prior evidence and read it directly."}],
                          "resolvedReferences": [],
                          "resolvedResources": [{
                            "label": "Adult Diabetes Eye Assessment Form",
                            "id": "eye-form",
                            "domainType": "DataModel",
                            "uri": "mauro-api://http-get/api/dataModels/eye-form",
                            "evidenceId": "${priorEvidenceId}",
                            "reason": "The form appears in prior resourceRefs from the previous search."
                          }],
                          "priorEvidenceToReuse": [{"evidenceId": "${priorEvidenceId}", "reason": "Contains the selected resource reference."}],
                          "priorGuidanceToFollow": [],
                          "planningHints": ["The resource is already identified; plan a read rather than another search."],
                          "constraints": ["Do not invent facts about the form."]
                        }""")])
                    case 'agent_planner':
                        plannerCalls++
                        plannerPrompts.add(request.messages.last().content)
                        if (plannerCalls == 1) {
                            return Flux.fromIterable([token(request, '''{
                              "goalRestatement":"Find forms about diabetes",
                              "fitness":"usable",
                              "successCriteria":["Evidence: matching form search results exist"],
                              "assumptions":[],
                              "risks":[],
                              "steps":[{
                                "title":"Search diabetes forms",
                                "objective":"Search for diabetes forms.",
                                "kind":"search",
                                "allowedTools":["mauro_search"],
                                "guard":"always",
                                "guardReason":"Primary search.",
                                "optional":false,
                                "expectedOutput":"Search results.",
                                "successCriteria":["Evidence: search result contains forms"]
                              }]
                            }''')])
                        }
                        return Flux.fromIterable([token(request, '''{
                          "goalRestatement":"Look more closely at the Adult Diabetes Eye Assessment Form",
                          "fitness":"usable",
                          "successCriteria":["Evidence: selected form details are retrieved"],
                          "assumptions":[],
                          "risks":[],
                          "steps":[{
                            "title":"Read Adult Diabetes Eye Assessment Form",
                            "objective":"Use mauro_get with the resolved resource URI from prior context.",
                            "kind":"read",
                            "allowedTools":["mauro_get"],
                            "guard":"always",
                            "guardReason":"The resource has already been identified by Perceive.",
                            "optional":false,
                            "expectedOutput":"Form details.",
                            "successCriteria":["Evidence: selected form details exist"]
                          }]
                        }''')])
                    case 'agent_executor':
                        executorCalls++
                        if (executorCalls == 1) {
                            return Flux.fromIterable([
                                new ProviderChunk('tool_call', request.messageId, null, [
                                    callId: 'call-search',
                                    name: 'mauro_search',
                                    arguments: [searchTerm: 'forms about diabetes']
                                ])
                            ])
                        }
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-get',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/eye-form']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "stepComplete": true,
                          "decision": "continue",
                          "summary": "The step completed.",
                          "reason": "Tool evidence exists.",
                          "question": null
                        }''')])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "decision": "final",
                          "summary": "The available evidence satisfies this turn.",
                          "reason": "The step completed and the goal can be answered.",
                          "question": null,
                          "missing": [],
                          "obsoleteStepIds": []
                        }''')])
                    case 'agent_final':
                        finalPrompts.add(request.messages.last().content)
                        return Flux.fromIterable([token(request, 'Form details answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [
                        new ToolSummaryDto(name: 'mauro_search', description: 'Search Mauro catalogue', inputSchema: [type: 'object']),
                        new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])
                    ]
                )
            ]
            invokeTool(_ as String, _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest toolRequest ->
                invokedTools.add(toolName)
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        arguments: toolRequest.arguments,
                        output: toolName == 'mauro_search'
                            ? [count: 1, items: [[label: 'Adult Diabetes Eye Assessment Form', id: 'eye-form', domainType: 'DataModel', readUri: 'mauro-api://http-get/api/dataModels/eye-form']]]
                            : [label: 'Adult Diabetes Eye Assessment Form', id: 'eye-form', domainType: 'DataModel']
                    ],
                    modelText: "Tool ${toolName} succeeded.".toString()
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            Stub(ChatPromptAssetService) {
                listAssetsByType('PERSONA') >> []
                listAssetsByType('SKILL') >> []
                searchAssets(_ as String) >> []
            },
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: 'session-resolved-resource', workspaceId: 'default', model: 'fake-model')

        when:
        Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Find forms about diabetes'),
            'assistant-resource-first',
            store.messagesForSession(session.id),
            null
        )).collectList().block()
        List<ChatEventDto> secondEvents = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: "Thanks. Let's look more closely at the Adult Diabetes Eye Assessment Form"),
            'assistant-resource-second',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        invokedTools == ['mauro_search', 'mauro_get']
        plannerPrompts.last().contains('Resolved resources:')
        plannerPrompts.last().contains('Adult Diabetes Eye Assessment Form')
        plannerPrompts.last().contains('mauro-api://http-get/api/dataModels/eye-form')
        finalPrompts.last().contains('Tool result mauro_get')
        !finalPrompts.last().contains('Tool result mauro_search')
        !finalPrompts.last().contains('mauro_search | final guidance')
        secondEvents.find {it.type == 'agent_context_resolved'}.metadata.resolvedResources.first().label == 'Adult Diabetes Eye Assessment Form'
        secondEvents.find {it.type == 'tool_call'}.metadata.name == 'mauro_get'
        secondEvents.find {it.type == 'token'}.content == 'Form details answer.'
    }

    void 'non retryable tool client error suppresses same step retry and replans'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<Map<String, Object>> invokedArguments = []
        int contextCalls = 0
        int plannerCalls = 0
        int executorCalls = 0
        int stepEvaluatorCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        contextCalls++
                        return Flux.fromIterable([token(request, """{
                          "goalRestatement": "Compare two diabetes forms",
                          "followUpInterpretation": "${contextCalls == 1 ? 'follow_up' : 'refinement'}",
                          "goalFrame": {
                            "userGoal": "Compare the Adult Diabetes Education Form to the Childrens Diabetes Education Form",
                            "interpretedGoal": "Read both resolved forms and compare them.",
                            "scope": "Two named form resources.",
                            "nonGoals": ["Do not retry a malformed URI."],
                            "acceptableCompletion": {
                              "mode": "answer_with_caveats",
                              "minimumEvidence": ["Both resources are read or a clear read failure is reported."],
                              "acceptableCaveats": ["One resource may need re-resolution if a read fails."],
                              "mustNotBlockOn": []
                            }
                          },
                          "domainContext": ["The form IDs came from prior search results."],
                          "relevantTools": [{"name": "mauro_get", "reason": "Read resolved resources", "readOnly": true}],
                          "recommendedSkills": [],
                          "relevantResources": [],
                          "instructions": [],
                          "resolvedReferences": [],
                          "resolvedResources": [],
                          "priorEvidenceToReuse": [],
                          "priorGuidanceToFollow": [],
                          "planningHints": ["Use exact resource URIs."],
                          "constraints": ["Do not invent facts."]
                        }""")])
                    case 'agent_planner':
                        plannerCalls++
                        return Flux.fromIterable([token(request, plannerCalls == 1 ? '''{
                          "goalRestatement":"Compare two diabetes forms",
                          "fitness":"usable",
                          "successCriteria":["Evidence: Adult form read succeeds","Evidence: Childrens form read succeeds","Comparison: Compare both forms"],
                          "assumptions":[],
                          "risks":[],
                          "steps":[{
                            "title":"Read Adult Diabetes Education Form",
                            "objective":"Read the Adult Diabetes Education Form.",
                            "kind":"read",
                            "allowedTools":["mauro_get"],
                            "guard":"always",
                            "guardReason":"Required resource read.",
                            "optional":false,
                            "expectedOutput":"Adult form details.",
                            "successCriteria":["Evidence: adult form details exist"]
                          }]
                        }''' : '''{
                          "goalRestatement":"Compare two diabetes forms",
                          "fitness":"usable",
                          "successCriteria":["Evidence: Adult form read succeeds","Answer: Explain available comparison evidence"],
                          "assumptions":[],
                          "risks":[],
                          "steps":[{
                            "title":"Read Adult Diabetes Education Form with corrected URI",
                            "objective":"Read the Adult Diabetes Education Form using the corrected URI.",
                            "kind":"read",
                            "allowedTools":["mauro_get"],
                            "guard":"always",
                            "guardReason":"The previous URI was malformed.",
                            "optional":false,
                            "expectedOutput":"Adult form details.",
                            "successCriteria":["Evidence: adult form details exist"]
                          }]
                        }''')])
                    case 'agent_executor':
                        executorCalls++
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: "call-${executorCalls}".toString(),
                                name: 'mauro_get',
                                arguments: [
                                    uri: executorCalls == 1
                                        ? 'mauro-api://http-get/api/dataModels/019ddee8-a68d-7fc9-b84b-019ddee8-a68d-7fc9-b84b-017d9e687f03'
                                        : 'mauro-api://http-get/api/dataModels/019ddee8-a68d-7fc9-b84b-017d9e687f03'
                                ]
                            ])
                        ])
                    case 'agent_step_evaluator':
                        stepEvaluatorCalls++
                        return Flux.fromIterable([token(request, stepEvaluatorCalls == 1 ? '''{
                          "stepComplete": false,
                          "decision": "retry",
                          "summary": "Read failed with a bad request.",
                          "reason": "The tool returned HTTP 400 for a malformed URI.",
                          "question": null
                        }''' : '''{
                          "stepComplete": true,
                          "decision": "continue",
                          "summary": "Read succeeded.",
                          "reason": "The corrected tool call returned the resource.",
                          "question": null
                        }''')])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "decision": "final",
                          "summary": "Enough evidence is available for this regression.",
                          "reason": "The corrected read succeeded.",
                          "question": null,
                          "missing": [],
                          "obsoleteStepIds": []
                        }''')])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Comparison answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])]
                )
            ]
            invokeTool('mauro_get', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest toolRequest ->
                invokedArguments.add(new LinkedHashMap<String, Object>(toolRequest.arguments ?: ([:] as Map<String, Object>)))
                boolean bad = invokedArguments.size() == 1
                new ToolInvokeResponse(
                    success: true,
                    result: [
                        tool: toolName,
                        arguments: toolRequest.arguments,
                        output: bad
                            ? [statusCode: 400, message: 'Bad Request', errors: [[message: 'UUID string too large']]]
                            : [statusCode: 200, label: 'Childrens Diabetes Education Form', id: '019ddee8-a68d-7fc9-b84b-017d9e687f03']
                    ],
                    modelText: bad ? 'Tool mauro_get completed with HTTP 400.' : 'Tool mauro_get completed with HTTP 200.'
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            Stub(ChatPromptAssetService) {
                listAssetsByType('PERSONA') >> []
                listAssetsByType('SKILL') >> []
                searchAssets(_ as String) >> []
            },
            4,
            8,
            3
        )
        SessionDto session = new SessionDto(id: 'session-nonretryable', workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Compare the Adult Diabetes Education Form to the Childrens Diabetes Education Form'),
            'assistant-nonretryable',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        invokedArguments*.uri == [
            'mauro-api://http-get/api/dataModels/019ddee8-a68d-7fc9-b84b-019ddee8-a68d-7fc9-b84b-017d9e687f03',
            'mauro-api://http-get/api/dataModels/019ddee8-a68d-7fc9-b84b-017d9e687f03'
        ]
        events*.type.contains('agent_step_retry_suppressed')
        !events*.type.contains('agent_step_retrying')
        events*.type.contains('agent_replan_started')
        events.find {it.type == 'token'}.content == 'Comparison answer.'
    }

    void 'context resolver can request older session memory page through formal sifter loop'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        String sessionId = 'session-context-sift'
        Instant now = Instant.now()
        seedPriorRunWithSearchEvidence(store, sessionId, 'old-run', 'Find older diabetes forms', 'Old Diabetes Education Form', 'old-form', now.minusSeconds(300))
        seedPriorRunWithSearchEvidence(store, sessionId, 'mid-run', 'Find unrelated forms', 'Middle Noise Form', 'mid-form', now.minusSeconds(200))
        seedPriorRunWithSearchEvidence(store, sessionId, 'recent-run', 'Find recent forms', 'Recent Noise Form', 'recent-form', now.minusSeconds(100))
        List<String> contextPrompts = []
        List<String> invokedTools = []
        int contextCalls = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest request ->
                String purpose = request.options?.purpose as String
                switch (purpose) {
                    case 'agent_context_resolver':
                        contextCalls++
                        String prompt = request.messages.last().content
                        contextPrompts.add(prompt)
                        if (!prompt.contains('label=Old Diabetes Education Form')) {
                            return Flux.fromIterable([token(request, '''{
                              "goalRestatement": "Pull up the Old Diabetes Education Form",
                              "followUpInterpretation": "details_request",
                              "goalFrame": {
                                "userGoal": "Pull up the Old Diabetes Education Form",
                                "interpretedGoal": "Find the prior resource reference and read it.",
                                "scope": "A prior session resource reference.",
                                "nonGoals": [],
                                "acceptableCompletion": {
                                  "mode": "answer_with_caveats",
                                  "minimumEvidence": ["The resource is found or an older page is requested."],
                                  "acceptableCaveats": [],
                                  "mustNotBlockOn": []
                                }
                              },
                              "domainContext": ["This is a follow-up to prior session results."],
                              "relevantTools": [{"name": "mauro_get", "reason": "Read resolved resource", "readOnly": true}],
                              "recommendedSkills": [],
                              "relevantResources": [],
                              "instructions": [],
                              "resolvedReferences": [],
                              "resolvedResources": [],
                              "priorEvidenceToReuse": [],
                              "priorGuidanceToFollow": [],
                              "contextRequests": [{"type": "session_resource_lookup", "query": "Old Diabetes Education Form", "reason": "The current continuity page did not contain the named resource."}],
                              "planningHints": ["Request older session memory."],
                              "constraints": ["Do not search the catalogue again just to identify prior context."]
                            }''')])
                        }
                        return Flux.fromIterable([token(request, '''{
                          "goalRestatement": "Pull up the Old Diabetes Education Form",
                          "followUpInterpretation": "details_request",
                          "goalFrame": {
                            "userGoal": "Pull up the Old Diabetes Education Form",
                            "interpretedGoal": "Read the prior resource reference.",
                            "scope": "The resolved prior resource.",
                            "nonGoals": ["Do not repeat the original search."],
                            "acceptableCompletion": {
                              "mode": "answer_with_caveats",
                              "minimumEvidence": ["The resource is read."],
                              "acceptableCaveats": [],
                              "mustNotBlockOn": []
                            }
                          },
                          "domainContext": ["The requested form was found in an older prior session page."],
                          "relevantTools": [{"name": "mauro_get", "reason": "Read resolved resource", "readOnly": true}],
                          "recommendedSkills": [],
                          "relevantResources": [],
                          "instructions": [{"type": "resolved_resource", "target": "planner", "instruction": "Use the resolved resource URI from prior session memory."}],
                          "resolvedReferences": [],
                          "resolvedResources": [{
                            "label": "Old Diabetes Education Form",
                            "id": "old-form",
                            "domainType": "DataModel",
                            "uri": "mauro-api://http-get/api/dataModels/old-form",
                            "evidenceId": "old-evidence",
                            "reason": "Found in older session memory page."
                          }],
                          "priorEvidenceToReuse": [{"evidenceId": "old-evidence", "reason": "Contains selected resource ref."}],
                          "priorGuidanceToFollow": [],
                          "contextRequests": [],
                          "planningHints": ["Read the resolved resource."],
                          "constraints": ["Do not broaden the goal."]
                        }''')])
                    case 'agent_planner':
                        return Flux.fromIterable([token(request, '''{
                          "goalRestatement":"Pull up the Old Diabetes Education Form",
                          "fitness":"usable",
                          "successCriteria":["Evidence: resolved form is read"],
                          "assumptions":[],
                          "risks":[],
                          "steps":[{
                            "title":"Read old form",
                            "objective":"Use mauro_get with the resolved URI from context.",
                            "kind":"read",
                            "allowedTools":["mauro_get"],
                            "guard":"always",
                            "guardReason":"Resolved resource should be read.",
                            "optional":false,
                            "expectedOutput":"Form details.",
                            "successCriteria":["Evidence: form details exist"]
                          }]
                        }''')])
                    case 'agent_executor':
                        return Flux.fromIterable([
                            new ProviderChunk('tool_call', request.messageId, null, [
                                callId: 'call-old-get',
                                name: 'mauro_get',
                                arguments: [uri: 'mauro-api://http-get/api/dataModels/old-form']
                            ])
                        ])
                    case 'agent_step_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "stepComplete": true,
                          "decision": "continue",
                          "summary": "Read succeeded.",
                          "reason": "The resource was read.",
                          "question": null
                        }''')])
                    case 'agent_plan_evaluator':
                        return Flux.fromIterable([token(request, '''{
                          "decision": "final",
                          "summary": "The requested resource was read.",
                          "reason": "The goal can be answered.",
                          "question": null,
                          "missing": [],
                          "obsoleteStepIds": []
                        }''')])
                    case 'agent_final':
                        return Flux.fromIterable([token(request, 'Old form answer.')])
                    default:
                        return Flux.empty()
                }
            }
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> [
                new McpServerDto(
                    id: 'local-mcp',
                    name: 'Local MCP',
                    tools: [new ToolSummaryDto(name: 'mauro_get', description: 'Read Mauro resource', inputSchema: [type: 'object'])]
                )
            ]
            invokeTool('mauro_get', _ as ToolInvokeRequest) >> {String toolName, ToolInvokeRequest toolRequest ->
                invokedTools.add(toolName)
                new ToolInvokeResponse(
                    success: true,
                    result: [tool: toolName, arguments: toolRequest.arguments, output: [label: 'Old Diabetes Education Form', id: 'old-form']],
                    modelText: 'Tool mauro_get completed with HTTP 200.'
                )
            }
        }
        AgentSupervisorService service = new AgentSupervisorService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            Stub(ChatPromptAssetService) {
                listAssetsByType('PERSONA') >> []
                listAssetsByType('SKILL') >> []
                searchAssets(_ as String) >> []
            },
            4,
            8,
            2
        )
        SessionDto session = new SessionDto(id: sessionId, workspaceId: 'default', model: 'fake-model')

        when:
        List<ChatEventDto> events = Flux.from(service.streamAgentRun(
            session,
            new SendMessageRequest(content: 'Pull up the Old Diabetes Education Form'),
            'assistant-context-sift',
            store.messagesForSession(session.id),
            null
        )).collectList().block()

        then:
        contextCalls == 2
        contextPrompts.first().contains('Session continuity page 0')
        !contextPrompts.first().contains('label=Old Diabetes Education Form')
        contextPrompts.last().contains('Session continuity page 1')
        contextPrompts.last().contains('label=Old Diabetes Education Form')
        events*.type.contains('agent_context_sifted')
        events.find {it.type == 'agent_operation_in_progress' && it.metadata.roleName == 'context_sifter'}
        events.find {it.type == 'agent_context_resolved'}.metadata.resolvedResources.first().label == 'Old Diabetes Education Form'
        invokedTools == ['mauro_get']
        events.find {it.type == 'token'}.content == 'Old form answer.'
    }

    private static ProviderChunk token(ProviderRequest request, String content) {
        new ProviderChunk('token', request.messageId, content, [:])
    }

    private static void seedPriorRunWithSearchEvidence(
        ChatInMemoryStore store,
        String sessionId,
        String runId,
        String goal,
        String label,
        String id,
        Instant createdAt
    ) {
        String planId = "${runId}-plan"
        store.agentRuns[runId] = new AgentRunRecord(
            id: runId,
            sessionId: sessionId,
            messageId: "${runId}-message",
            goal: goal,
            status: 'completed',
            model: 'fake-model',
            currentPlanId: planId,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        store.agentPlans[planId] = new AgentPlanRecord(
            id: planId,
            runId: runId,
            status: 'complete',
            goalRestatement: goal,
            successCriteria: ['Evidence: search results exist'],
            createdAt: createdAt
        )
        store.agentEvidence["${id == 'old-form' ? 'old' : id}-evidence"] = new AgentEvidenceRecord(
            id: id == 'old-form' ? 'old-evidence' : "${id}-evidence",
            runId: runId,
            stepId: "${runId}-step",
            sourceType: 'tool_result',
            sourceName: 'mauro_search',
            sourceId: "${runId}-call",
            title: 'Tool result mauro_search',
            summary: 'Tool mauro_search completed.',
            content: '',
            structuredContent: [
                callId: "${runId}-call",
                ok: true,
                output: [
                    tool: 'mauro_search',
                    arguments: [searchTerm: goal],
                    output: [
                        count: 1,
                        items: [[
                            label: label,
                            id: id,
                            domainType: 'DataModel',
                            readUri: "mauro-api://http-get/api/dataModels/${id}".toString()
                        ]]
                    ]
                ]
            ] as Map<String, Object>,
            createdAt: createdAt,
            metadata: [
                callId: "${runId}-call",
                ok: true,
                evidenceRole: 'tool_result',
                pertinentToFinal: true
            ] as Map<String, Object>
        )
    }

    private static String contextJson() {
        '''{
          "goalRestatement": "Resolve Mauro catalogue context",
          "domainContext": ["Mauro catalogue"],
          "relevantTools": [{"name": "mauro_search", "reason": "Search catalogue resources", "readOnly": true}],
          "recommendedSkills": [],
          "relevantResources": [],
          "planningHints": ["Search labels before reading resources."],
          "constraints": ["Use typed URIs from tool results."]
        }'''
    }

    private ChatPromptAssetService formSkillService() {
        Stub(ChatPromptAssetService) {
            listAssetsByType('PERSONA') >> []
            listAssetsByType('SKILL') >> [
                new ChatPromptAssetDefinition(
                    id: 'mauro-form-representation',
                    name: 'Mauro Form Representation',
                    type: 'SKILL',
                    priority: 1,
                    description: 'Interpreting form language as Mauro catalogue concepts',
                    keywords: ['form', 'forms'],
                    toolApplicability: [
                        new SkillToolApplicability(
                            tool: 'mauro_search',
                            relationship: 'REQUIRED_PREREQUISITE',
                            triggerTerms: ['form', 'forms'],
                            instructions: ['Forms map to DataModel']
                        )
                    ],
                    instruction: '''Internal decision-making context.
Search parameter rules:
- "forms about X" -> mauro_search domainTypes ["DataModel"].
                    - "questions about X" -> mauro_search domainTypes ["DataElement"].'''
                )
            ]
            searchAssets(_ as String) >> []
        }
    }
}
