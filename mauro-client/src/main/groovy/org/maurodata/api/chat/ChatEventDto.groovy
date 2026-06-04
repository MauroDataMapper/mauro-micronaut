package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ChatEventDto {
    String type
    String messageId
    String role
    String content
    Boolean done
    Map<String, Object> metadata = [:]
}
