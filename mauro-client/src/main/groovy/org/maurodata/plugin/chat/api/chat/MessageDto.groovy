package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class MessageDto {
    String id
    String sessionId
    String role
    String content
    String status
    String thinkingContent
    String createdAt
    String updatedAt
    Map<String, Object> metadata = [:]
}
