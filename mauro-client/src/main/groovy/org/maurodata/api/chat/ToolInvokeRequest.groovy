package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ToolInvokeRequest {
    Map<String, Object> arguments = [:]
    Map<String, List<String>> forwardHeaders = [:]
}
