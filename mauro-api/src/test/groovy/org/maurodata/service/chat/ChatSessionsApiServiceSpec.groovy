package org.maurodata.service.chat

import org.maurodata.api.chat.MessageDto
import org.maurodata.api.chat.SendMessageRequest
import org.maurodata.service.chat.llm.LlmProvider
import org.maurodata.service.chat.llm.ProviderChunk
import org.maurodata.service.chat.llm.ProviderMessage
import org.maurodata.service.chat.llm.ProviderRegistry
import org.maurodata.service.chat.llm.ProviderRequest
import reactor.core.publisher.Flux
import spock.lang.Specification

import java.lang.reflect.Method

class ChatSessionsApiServiceSpec extends Specification {

    void 'provider history replays hidden provider state in provider order'() {
        given:
        List<MessageDto> timeline = [
            new MessageDto(
                role: 'user',
                content: 'Find forms about diabetes',
                status: 'complete',
                metadata: [
                    providerMessagesBefore: [
                        [role: 'system', content: 'persona context'],
                        [role: 'system', content: 'routing context']
                    ]
                ]
            ),
            new MessageDto(
                role: 'assistant',
                content: 'I found forms about diabetes.',
                status: 'complete',
                metadata: [
                    providerMessages: [
                        [
                            role: 'assistant',
                            content: '',
                            toolCalls: [[
                                type: 'function',
                                function: [name: 'catalogue_search', arguments: [searchTerm: 'diabetes', domainTypes: ['DataModel']]]
                            ]]
                        ],
                        [role: 'tool', name: 'catalogue_search', content: 'Tool catalogue_search succeeded.'],
                        [role: 'system', content: 'post tool result context']
                    ]
                ]
            )
        ]

        when:
        List<ProviderMessage> history = buildProviderHistory(timeline)

        then:
        history*.role == ['system', 'system', 'user', 'assistant', 'tool', 'system', 'assistant']
        history*.content == [
            'persona context',
            'routing context',
            'Find forms about diabetes',
            '',
            'Tool catalogue_search succeeded.',
            'post tool result context',
            'I found forms about diabetes.'
        ]
        history[3].toolCalls[0].function.name == 'catalogue_search'
        history[4].name == 'catalogue_search'
    }

    void 'stored messages preserve event order and keep thinking separate from final assistant content'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> Flux.fromIterable([
                new ProviderChunk('thinking_start', 'assistant-ignored', '', [:]),
                new ProviderChunk('thinking_token', 'assistant-ignored', 'thinking first', [:]),
                new ProviderChunk('thinking_token', 'assistant-ignored', ' thinking second', [:]),
                new ProviderChunk('thinking_end', 'assistant-ignored', '', [:]),
                new ProviderChunk('tool_call', 'assistant-ignored', null, [callId: 'call-1', name: 'catalogue_search', arguments: [searchTerm: 'diabetes']]),
                new ProviderChunk('tool_result', 'assistant-ignored', null, [callId: 'call-1', ok: true, output: [count: 1]]),
                new ProviderChunk('token', 'assistant-ignored', 'final ', [:]),
                new ProviderChunk('token', 'assistant-ignored', 'answer', [:])
            ])
        }
        ChatSkillService skillService = Stub(ChatSkillService) {
            listSkillDefinitions() >> []
            listPersonaDefinitions() >> [
                new ChatSkillDefinition(id: 'mauro-catalogue', name: 'Mauro Catalogue', type: 'PERSONA', instruction: 'persona context')
            ]
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> []
        }
        ChatSessionsApiService service = new ChatSessionsApiService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            skillService,
            new ChatPromptResourceService(),
            'llama3.1'
        )
        String sessionId = service.createSession(new org.maurodata.api.chat.CreateSessionRequest(workspaceId: 'default')).id

        when:
        Flux.from(service.sendMessage(sessionId, new SendMessageRequest(messageId: 'user-1', content: 'Find diabetes'))).collectList().block()
        List<MessageDto> messages = service.listSessionMessages(sessionId, 50, null).items

        then:
        messages.collect {MessageDto message -> message.metadata?.eventType ?: message.role} == [
            'ui_user_message',
            'message_start',
            'provider_request_message',
            'provider_request_message',
            'provider_request_message',
            'thinking_start',
            'thinking_token',
            'thinking_end',
            'tool_call',
            'tool_result',
            'token',
            'message_complete',
            'done'
        ]
        messages.find {it.metadata?.eventType == 'ui_user_message'}.content == 'Find diabetes'
        List<MessageDto> providerRequestMessages = messages.findAll {it.metadata?.eventType == 'provider_request_message'}
        providerRequestMessages*.role == ['system', 'system', 'user']
        providerRequestMessages[0].content == 'persona context'
        providerRequestMessages[0].metadata.source == 'persona'
        providerRequestMessages[0].metadata.replayMode == 'substitute'
        providerRequestMessages[0].metadata.substitutionKey == 'persona:active'
        providerRequestMessages[1].content.startsWith('Routing index for available Mauro assistance.')
        providerRequestMessages[1].metadata.source == 'routing'
        providerRequestMessages[1].metadata.replayMode == 'substitute'
        providerRequestMessages[1].metadata.substitutionKey == 'routing:index'
        providerRequestMessages[2].content == 'Find diabetes'
        providerRequestMessages[2].metadata.source == 'projected_history'
        providerRequestMessages[2].metadata.replayMode == 'omit'
        messages.find {it.metadata?.eventType == 'thinking_token'}.content == 'thinking first thinking second'
        messages.find {it.metadata?.eventType == 'token'}.content == 'final answer'
        messages.find {it.metadata?.eventType == 'tool_call'}.metadata.arguments == [searchTerm: 'diabetes']
    }

    void 'continuing a session sends prior user and assistant turns to the LLM'() {
        given:
        ChatInMemoryStore store = new ChatInMemoryStore()
        List<List<Map<String, String>>> providerRequests = []
        int callCount = 0
        LlmProvider provider = Stub(LlmProvider) {
            id() >> 'ollama'
            streamChat(_ as ProviderRequest) >> {ProviderRequest providerRequest ->
                providerRequests.add(providerRequest.messages.collect {ProviderMessage message ->
                    [role: message.role, content: message.content]
                })
                callCount++
                Flux.fromIterable([
                    new ProviderChunk('token', providerRequest.messageId, callCount == 1 ? 'first answer' : 'second answer', [:])
                ])
            }
        }
        ChatSkillService skillService = Stub(ChatSkillService) {
            listSkillDefinitions() >> []
            listPersonaDefinitions() >> []
        }
        ChatMcpService mcpService = Stub(ChatMcpService) {
            listServers() >> []
        }
        ChatSessionsApiService service = new ChatSessionsApiService(
            store,
            new ProviderRegistry([provider]),
            mcpService,
            skillService,
            new ChatPromptResourceService(),
            'llama3.1'
        )
        String sessionId = service.createSession(new org.maurodata.api.chat.CreateSessionRequest(workspaceId: 'default')).id

        when:
        Flux.from(service.sendMessage(sessionId, new SendMessageRequest(messageId: 'user-1', content: 'First question'))).collectList().block()
        Flux.from(service.sendMessage(sessionId, new SendMessageRequest(messageId: 'user-2', content: 'Second question'))).collectList().block()

        then:
        providerRequests.size() == 2
        providerRequests[0]*.role == ['system', 'user']
        providerRequests[0][0].content.startsWith('Routing index for available Mauro assistance.')
        providerRequests[0][1] == [role: 'user', content: 'First question']

        providerRequests[1]*.role == ['system', 'user', 'assistant', 'user']
        providerRequests[1][0].content.startsWith('Routing index for available Mauro assistance.')
        providerRequests[1][1] == [role: 'user', content: 'First question']
        providerRequests[1][2] == [role: 'assistant', content: 'first answer']
        providerRequests[1][3] == [role: 'user', content: 'Second question']
    }

    @SuppressWarnings('unchecked')
    private static List<ProviderMessage> buildProviderHistory(List<MessageDto> timeline) {
        Method method = ChatSessionsApiService.getDeclaredMethod('buildProviderHistory', List)
        method.accessible = true
        (List<ProviderMessage>) method.invoke(null, timeline)
    }
}
