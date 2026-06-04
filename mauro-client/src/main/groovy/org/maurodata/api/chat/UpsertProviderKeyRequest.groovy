package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank

@Introspected
@CompileStatic
class UpsertProviderKeyRequest {
    @NotBlank String apiKey
}
