package org.maurodata.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.reactivestreams.Publisher

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.core.annotation.Nullable
import jakarta.validation.Valid

@MauroApi
interface ChatSessionsApi {

    @Post(Paths.CHAT_SESSIONS)
    SessionDto createSession(@Body @Valid CreateSessionRequest request)

    @Get(Paths.CHAT_SESSIONS_ID)
    SessionDto getSession(@PathVariable String sessionId)

    @Patch(Paths.CHAT_SESSIONS_UPDATE)
    SessionDto updateSession(@PathVariable String sessionId, @Body @Valid UpdateSessionRequest request)

    @Post(uri = Paths.CHAT_SESSIONS_MESSAGES, produces = MediaType.TEXT_EVENT_STREAM)
    Publisher<ChatEventDto> sendMessage(@PathVariable String sessionId, @Body @Valid SendMessageRequest request)

    @Get(Paths.CHAT_SESSIONS_MESSAGES_LIST)
    ListSessionMessagesResponseDto listSessionMessages(
        @PathVariable String sessionId,
        @QueryValue(defaultValue = '200') Integer limit,
        @Nullable @QueryValue String beforeMessageId
    )
}
