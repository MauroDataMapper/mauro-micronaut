package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

@Introspected
@CompileStatic
class CreateSessionRequest {
    @NotBlank String workspaceId
    String title
    String model
    List<String> skillIds = []
    Map<String, Object> metadata = [:]
}
