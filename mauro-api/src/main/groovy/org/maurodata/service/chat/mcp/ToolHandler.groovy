package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic

@CompileStatic
interface ToolHandler {
    String name()
    String description()
    Map<String, Object> inputSchema()
    Map<String, Object> routing()
    Map<String, Object> invoke(Map<String, Object> arguments)
    String modelText(Map<String, Object> result)
}
