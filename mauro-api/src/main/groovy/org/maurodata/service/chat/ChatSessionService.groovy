package org.maurodata.service.chat

import org.maurodata.api.chat.ChatEventDto
import org.maurodata.api.chat.CreateSessionRequest
import org.maurodata.api.chat.ListSessionMessagesResponseDto
import org.maurodata.api.chat.SendMessageRequest
import org.maurodata.api.chat.SessionDto
import org.reactivestreams.Publisher

interface ChatSessionService {
    SessionDto createSession(CreateSessionRequest request)
    SessionDto getSession(String sessionId)
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request)
    ListSessionMessagesResponseDto listSessionMessages(String sessionId, Integer limit, String beforeMessageId)
}
