package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.core.annotation.Nullable
import jakarta.validation.Valid
import org.maurodata.api.Paths
import org.maurodata.api.chat.ChatEventDto
import org.maurodata.api.chat.ChatSessionsApi
import org.maurodata.api.chat.CreateSessionRequest
import org.maurodata.api.chat.ListSessionMessagesResponseDto
import org.maurodata.api.chat.SendMessageRequest
import org.maurodata.api.chat.SessionDto
import org.maurodata.api.chat.UpdateSessionSkillsRequest
import org.maurodata.audit.Audit
import org.maurodata.service.chat.ChatSessionService
import org.reactivestreams.Publisher

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ChatSessionsController implements ChatSessionsApi {

    private final ChatSessionService chatSessionService

    ChatSessionsController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.CHAT_SESSIONS)
    SessionDto createSession(@Body @Valid CreateSessionRequest request) {
        chatSessionService.createSession(request)
    }

    @Override
    @Audit
    @Get(Paths.CHAT_SESSIONS_ID)
    SessionDto getSession(@PathVariable String sessionId) {
        chatSessionService.getSession(sessionId)
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(uri = Paths.CHAT_SESSIONS_MESSAGES, produces = MediaType.TEXT_EVENT_STREAM)
    Publisher<ChatEventDto> sendMessage(@PathVariable String sessionId, @Body @Valid SendMessageRequest request) {
        chatSessionService.sendMessage(sessionId, request)
    }

    @Override
    @Audit
    @Get(Paths.CHAT_SESSIONS_MESSAGES_LIST)
    ListSessionMessagesResponseDto listSessionMessages(
        @PathVariable String sessionId,
        Integer limit,
        @Nullable String beforeMessageId
    ) {
        chatSessionService.listSessionMessages(sessionId, limit, beforeMessageId)
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Status(HttpStatus.NO_CONTENT)
    @Put(Paths.CHAT_SESSIONS_SKILLS)
    void updateSessionSkills(@PathVariable String sessionId, @Body @Valid UpdateSessionSkillsRequest request) {
        chatSessionService.updateSessionSkills(sessionId, request)
    }
}
