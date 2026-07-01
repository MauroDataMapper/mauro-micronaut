package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Introspected
@CompileStatic
class AttachmentRef {
    @NotBlank String id
    @NotBlank String name
    @NotBlank String mediaType
    @NotNull Long sizeBytes
    String url
}
