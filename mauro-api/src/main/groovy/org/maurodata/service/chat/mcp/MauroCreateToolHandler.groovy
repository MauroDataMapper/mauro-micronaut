package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_create',
    description = 'Create a Mauro API resource through a confirmed create operation.',
    purpose = 'Execute a route described as a create operation after schema inspection and explicit confirmation.',
    useWhen = [
        'creating a new Mauro resource after mauro_describe identifies a create operation',
        'executing a confirmed POST create route with required path parameters and JSON body'
    ],
    avoidWhen = [
        'searching or listing resources; use mauro_keyword_search or mauro_list',
        'reading resources; use mauro_get',
        'updating or deleting resources; use mauro_update or mauro_delete',
        'calling import/export/search POST routes that are not described as create operations'
    ],
    examples = [
        'create DataModel in a known folder after inspecting schema',
        'call a confirmed create operation URI with folderId and body'
    ],
    readOnlyHint = false,
    destructiveHint = false,
    idempotentHint = false,
    openWorldHint = true,
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Resource type to create, for example DataModel or Terminology."},"operationUri":{"type":"string","description":"Create operation URI returned by mauro_describe."},"path":{"type":"string","description":"Concrete or template route path. Template placeholders must be supplied in pathParameters."},"pathParameters":{"type":"object","additionalProperties":true,"description":"Values for route placeholders such as folderId."},"body":{"type":"object","additionalProperties":true,"description":"JSON request body matching mauro_schema for the create operation."},"confirmed":{"type":"boolean","description":"Must be true before the create operation executes."}}}'
)
class MauroCreateToolHandler extends AbstractMauroWriteToolHandler {

    MauroCreateToolHandler(McpHttpResourceRegistry resourceRegistry, EmbeddedServer embeddedServer) {
        super(MauroCreateToolHandler, resourceRegistry, embeddedServer, 'create', 'POST')
    }
}
