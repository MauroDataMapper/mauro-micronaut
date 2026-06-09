package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class McpServerDto {
    String id
    String name
    String transport // STDIO | HTTP | SSE
    String url
    String level // GLOBAL | WORKSPACE
    String status // CONNECTED | DISCONNECTED | ERROR
    List<ToolSummaryDto> tools = []
}
