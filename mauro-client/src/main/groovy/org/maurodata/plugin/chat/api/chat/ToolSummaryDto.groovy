package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ToolSummaryDto {
    String name
    String description
    Map<String, Object> inputSchema = [:]
    Map<String, Object> routing = [:]
    Map<String, Object> annotations = [:]
}
