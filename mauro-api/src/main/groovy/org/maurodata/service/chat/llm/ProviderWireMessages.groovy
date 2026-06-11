package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
final class ProviderWireMessages {

    private ProviderWireMessages() {
    }

    static List<Map<String, Object>> toWireMessages(final List<ProviderMessage> messages, final boolean ollamaToolNames) {
        final List<ProviderMessage> coalesced = coalesceAdjacentSystemMessages(messages)
        final List<Map<String, Object>> wireMessages = new ArrayList<Map<String, Object>>(coalesced.size())
        for (int i = 0; i < coalesced.size(); i++) {
            final ProviderMessage m = coalesced.get(i)
            final Map<String, Object> wm = new LinkedHashMap<String, Object>(4)
            wm.put('role', m.role)
            wm.put('content', m.content)
            if (m.name != null) {
                if (ollamaToolNames && 'tool'.equals(m.role)) {
                    wm.put('tool_name', m.name)
                } else {
                    wm.put('name', m.name)
                }
            }
            if (m.toolCallId != null) {
                wm.put('tool_call_id', m.toolCallId)
            }
            if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
                wm.put('tool_calls', m.toolCalls)
            }
            wireMessages.add(wm)
        }
        wireMessages
    }

    static List<ProviderMessage> coalesceAdjacentSystemMessages(final List<ProviderMessage> messages) {
        final List<ProviderMessage> coalesced = new ArrayList<ProviderMessage>()
        if (messages == null || messages.isEmpty()) {
            return coalesced
        }

        final StringBuilder systemBuffer = new StringBuilder(2048)
        for (int i = 0; i < messages.size(); i++) {
            final ProviderMessage message = messages.get(i)
            if (message == null) {
                continue
            }
            if ('system'.equals(message.role) && message.toolCallId == null && message.name == null &&
                (message.toolCalls == null || message.toolCalls.isEmpty())) {
                if (systemBuffer.length() > 0) {
                    systemBuffer.append('\n\n')
                }
                systemBuffer.append(message.content ?: '')
                continue
            }
            flushSystemBuffer(coalesced, systemBuffer)
            coalesced.add(message)
        }
        flushSystemBuffer(coalesced, systemBuffer)
        coalesced
    }

    private static void flushSystemBuffer(final List<ProviderMessage> messages, final StringBuilder systemBuffer) {
        if (systemBuffer.length() == 0) {
            return
        }
        messages.add(new ProviderMessage(role: 'system', content: systemBuffer.toString()))
        systemBuffer.setLength(0)
    }
}
