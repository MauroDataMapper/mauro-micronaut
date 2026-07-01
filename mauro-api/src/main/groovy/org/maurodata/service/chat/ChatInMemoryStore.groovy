package org.maurodata.service.chat

import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.MessageDto
import org.maurodata.plugin.chat.api.chat.ProviderKeyStatusDto
import org.maurodata.plugin.chat.api.chat.SessionDto

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Singleton
class ChatInMemoryStore {
    final Map<String, SessionDto> sessions = new ConcurrentHashMap<>()
    final Map<String, ProviderKeyStatusDto> providerKeyStatus = new ConcurrentHashMap<>()
    final Map<String, List<MessageDto>> sessionMessages = new ConcurrentHashMap<>()

    static Instant now() {
        Instant.now()
    }

    List<MessageDto> messagesForSession(String sessionId) {
        sessionMessages.computeIfAbsent(sessionId) { new CopyOnWriteArrayList<MessageDto>() }
    }
}
