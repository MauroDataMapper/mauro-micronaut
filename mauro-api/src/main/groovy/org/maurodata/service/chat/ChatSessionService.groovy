package org.maurodata.service.chat

import org.maurodata.api.chat.ChatEventDto
import org.maurodata.api.chat.CreateSessionRequest
import org.maurodata.api.chat.ListSessionMessagesResponseDto
import org.maurodata.api.chat.SendMessageRequest
import org.maurodata.api.chat.SessionDto
import org.maurodata.api.chat.UpdateSessionRequest
import io.micronaut.http.HttpRequest
import org.reactivestreams.Publisher

interface ChatSessionService {
    SessionDto createSession(CreateSessionRequest request)
    SessionDto getSession(String sessionId)
    SessionDto updateSession(String sessionId, UpdateSessionRequest request)
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request)
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request, HttpRequest<?> httpRequest)
    ListSessionMessagesResponseDto listSessionMessages(String sessionId, Integer limit, String beforeMessageId)
}
