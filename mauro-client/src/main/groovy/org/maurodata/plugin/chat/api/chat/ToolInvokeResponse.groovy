package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ToolInvokeResponse {
    Boolean success
    Map<String, Object> result = [:]
    String modelText
    String error
}
