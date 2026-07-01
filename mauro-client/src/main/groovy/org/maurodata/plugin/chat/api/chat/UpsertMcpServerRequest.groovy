package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

@Introspected
@CompileStatic
class UpsertMcpServerRequest {
    String id
    @NotBlank String name
    @NotBlank String url
    Boolean enabled = Boolean.TRUE
    Map<String, Object> metadata = [:]
}
