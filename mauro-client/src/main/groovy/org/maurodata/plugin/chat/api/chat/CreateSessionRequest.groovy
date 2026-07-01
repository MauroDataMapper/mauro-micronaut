package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

@Introspected
@CompileStatic
class CreateSessionRequest {
    @NotBlank String workspaceId
    String title
    String model
    Map<String, Object> metadata = [:]
}
