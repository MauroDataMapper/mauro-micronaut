package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

import java.time.Instant

@Introspected
@CompileStatic
class SessionDto {
    @NotBlank String id
    @NotBlank String workspaceId
    String title
    @NotBlank String status // ACTIVE | ARCHIVED
    String model
    List<String> skillIds = []
    Instant createdAt
    Instant updatedAt
}
