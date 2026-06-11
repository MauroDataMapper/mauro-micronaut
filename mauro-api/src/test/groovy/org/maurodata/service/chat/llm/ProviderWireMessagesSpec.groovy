package org.maurodata.service.chat.llm

import spock.lang.Specification

class ProviderWireMessagesSpec extends Specification {

    void 'adjacent system messages are coalesced without changing transcript boundaries'() {
        given:
        List<ProviderMessage> messages = [
            new ProviderMessage(role: 'system', content: 'persona'),
            new ProviderMessage(role: 'system', content: 'routing'),
            new ProviderMessage(role: 'user', content: 'Find forms about maternity'),
            new ProviderMessage(role: 'system', content: 'post tool guidance'),
            new ProviderMessage(role: 'tool', content: 'Tool result', toolCallId: 'call-1', name: 'catalogue_search'),
            new ProviderMessage(role: 'system', content: 'final instruction')
        ]

        when:
        List<Map<String, Object>> wireMessages = ProviderWireMessages.toWireMessages(messages, true)

        then:
        wireMessages*.role == ['system', 'user', 'system', 'tool', 'system']
        wireMessages*.content == [
            'persona\n\nrouting',
            'Find forms about maternity',
            'post tool guidance',
            'Tool result',
            'final instruction'
        ]
        wireMessages[3].tool_name == 'catalogue_search'
        wireMessages[3].tool_call_id == 'call-1'
    }
}
