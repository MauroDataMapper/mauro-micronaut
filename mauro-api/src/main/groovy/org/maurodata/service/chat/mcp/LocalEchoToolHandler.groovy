package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'echo',
    description = 'Echo tool',
    purpose = 'Development and connectivity test tool that returns the supplied arguments.',
    useWhen = ['testing tool invocation or MCP connectivity'],
    avoidWhen = ['answering user catalogue, documentation, or Mauro guidance questions'],
    inputSchema = '{"type":"object"}'
)
class LocalEchoToolHandler extends AbstractAnnotatedToolHandler {

    LocalEchoToolHandler() {
        super(LocalEchoToolHandler)
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        [echo: arguments ?: [:]]
    }
}
