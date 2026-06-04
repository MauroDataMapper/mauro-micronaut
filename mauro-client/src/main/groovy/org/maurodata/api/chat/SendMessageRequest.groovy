package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

@Introspected
@CompileStatic
class SendMessageRequest {
    @NotBlank String messageId
    @NotBlank String role // user
    @NotBlank String content
    List<AttachmentRef> attachments = []
    List<String> contextRefs = [] // file/db/resource refs
    Map<String, Object> options = [:] // temperature, maxTokens, etc
}
