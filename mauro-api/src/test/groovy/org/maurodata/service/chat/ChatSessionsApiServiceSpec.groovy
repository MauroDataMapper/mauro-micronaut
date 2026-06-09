package org.maurodata.service.chat

import org.maurodata.api.chat.MessageDto
import org.maurodata.api.chat.ChatEventDto
import org.maurodata.service.chat.llm.ProviderMessage
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

    void 'provider system messages are exposed as system message events'() {
        given:
        List<ProviderMessage> messages = [
            new ProviderMessage(role: 'system', content: 'persona context'),
            new ProviderMessage(role: 'assistant', content: 'hidden assistant tool-call context')
        ]

        when:
        List<ChatEventDto> events = providerMessagesToEvents(messages)

        then:
        events*.type == ['system_message', 'provider_message']
        events*.role == ['system', 'assistant']
        events*.content == ['persona context', 'hidden assistant tool-call context']
        events[0].metadata.source == 'current_context'
        events[0].metadata.providerMessage.role == 'system'
        events[0].metadata.providerMessage.content == 'persona context'
    }

    void 'system provider messages are persisted as timeline events'() {
        given:
        List<MessageDto> timeline = []

        when:
        storeProviderMessageEvent(timeline, [
            providerMessage: [role: 'system', content: 'post tool result context']
        ] as Map<String, Object>)

        then:
        timeline.size() == 1
        timeline[0].role == 'system'
        timeline[0].status == 'event'
        timeline[0].content == 'post tool result context'
        timeline[0].metadata.eventType == 'system_message'
        timeline[0].metadata.source == 'tool_loop'
    }

    @SuppressWarnings('unchecked')
    private static List<ProviderMessage> buildProviderHistory(List<MessageDto> timeline) {
        Method method = ChatSessionsApiService.getDeclaredMethod('buildProviderHistory', List)
        method.accessible = true
        (List<ProviderMessage>) method.invoke(null, timeline)
    }

    @SuppressWarnings('unchecked')
    private static List<ChatEventDto> providerMessagesToEvents(List<ProviderMessage> messages) {
        Method method = ChatSessionsApiService.getDeclaredMethod('providerMessagesToEvents', List, String, String)
        method.accessible = true
        (List<ChatEventDto>) method.invoke(null, messages, 'assistant-1', 'current_context')
    }

    private static void storeProviderMessageEvent(List<MessageDto> timeline, Map<String, Object> metadata) {
        Method method = ChatSessionsApiService.getDeclaredMethod('storeProviderMessageEvent', List, String, String, Map, String)
        method.accessible = true
        method.invoke(null, timeline, 'session-1', 'assistant-1', metadata, 'tool_loop')
    }
}
