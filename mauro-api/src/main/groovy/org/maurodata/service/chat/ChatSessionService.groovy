package org.maurodata.service.chat

import org.maurodata.plugin.chat.api.chat.ChatEventDto
import org.maurodata.plugin.chat.api.chat.CreateSessionRequest
import org.maurodata.plugin.chat.api.chat.ListSessionMessagesResponseDto
import org.maurodata.plugin.chat.api.chat.SendMessageRequest
import org.maurodata.plugin.chat.api.chat.SessionDto
import org.maurodata.plugin.chat.api.chat.UpdateSessionRequest

import groovy.transform.CompileStatic
import io.micronaut.http.HttpRequest
import org.reactivestreams.Publisher

@CompileStatic
interface ChatSessionService {
    SessionDto createSession(CreateSessionRequest request)
    SessionDto getSession(String sessionId)
    SessionDto updateSession(String sessionId, UpdateSessionRequest request)
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request)
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request, HttpRequest<?> httpRequest)
    ListSessionMessagesResponseDto listSessionMessages(String sessionId, Integer limit, String beforeMessageId)
}
