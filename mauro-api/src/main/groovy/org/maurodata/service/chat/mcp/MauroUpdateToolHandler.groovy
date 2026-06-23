package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_update',
    description = 'Update a Mauro API resource through a confirmed update operation.',
    purpose = 'Execute a route described as an update operation after schema inspection and explicit confirmation.',
    useWhen = [
        'updating an existing Mauro resource after mauro_describe identifies an update operation',
        'executing a confirmed PUT update route with required path parameters and JSON body'
    ],
    avoidWhen = [
        'searching or listing resources; use mauro_keyword_search or mauro_list',
        'reading resources; use mauro_get',
        'creating or deleting resources; use mauro_create or mauro_delete',
        'calling finalise, move, merge, permission, or other PUT actions that are not described as ordinary update operations'
    ],
    examples = [
        'update DataModel by id after inspecting schema',
        'call a confirmed update operation URI with id and body'
    ],
    readOnlyHint = false,
    destructiveHint = false,
    idempotentHint = true,
    openWorldHint = true,
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Resource type to update, for example DataModel or Terminology."},"operationUri":{"type":"string","description":"Update operation URI returned by mauro_describe."},"path":{"type":"string","description":"Concrete or template route path. Template placeholders must be supplied in pathParameters."},"pathParameters":{"type":"object","additionalProperties":true,"description":"Values for route placeholders such as id."},"body":{"type":"object","additionalProperties":true,"description":"JSON request body matching mauro_schema for the update operation."},"confirmed":{"type":"boolean","description":"Must be true before the update operation executes."}}}'
)
class MauroUpdateToolHandler extends AbstractMauroWriteToolHandler {

    MauroUpdateToolHandler(McpHttpResourceRegistry resourceRegistry, EmbeddedServer embeddedServer) {
        super(MauroUpdateToolHandler, resourceRegistry, embeddedServer, 'update', 'PUT')
    }
}
