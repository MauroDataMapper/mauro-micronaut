package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProviderDto {
    String id // openai, anthropic, local
    String status // CONNECTED | MISSING_KEY | ERROR
    String message
}
