package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.transform.CompileStatic
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_delete',
    description = 'Delete a Mauro API resource through a confirmed delete operation.',
    purpose = 'Execute a route described as a delete operation only after explicit confirmation.',
    useWhen = [
        'deleting an existing Mauro resource after mauro_describe identifies a delete operation',
        'executing a confirmed DELETE route with required path parameters'
    ],
    avoidWhen = [
        'searching, listing, reading, creating, or updating resources',
        'calling non-delete actions that happen to use DELETE but are not described as delete operations',
        'executing without explicit user confirmation'
    ],
    examples = [
        'delete DataModel by id after explicit confirmation',
        'call a confirmed delete operation URI with id'
    ],
    readOnlyHint = false,
    destructiveHint = true,
    idempotentHint = false,
    openWorldHint = true,
    inputSchema = '{"type":"object","properties":{"resourceType":{"type":"string","description":"Resource type to delete, for example DataModel or Terminology."},"operationUri":{"type":"string","description":"Delete operation URI returned by mauro_describe."},"path":{"type":"string","description":"Concrete or template route path. Template placeholders must be supplied in pathParameters."},"pathParameters":{"type":"object","additionalProperties":true,"description":"Values for route placeholders such as id."},"body":{"type":"object","additionalProperties":true,"description":"Optional JSON request body if the delete route accepts one."},"confirmed":{"type":"boolean","description":"Must be true before the delete operation executes."}}}'
)
class MauroDeleteToolHandler extends AbstractMauroWriteToolHandler {

    MauroDeleteToolHandler(McpHttpResourceRegistry resourceRegistry, EmbeddedServer embeddedServer) {
        super(MauroDeleteToolHandler, resourceRegistry, embeddedServer, 'delete', 'DELETE')
    }
}
