package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.McpHttpResourceRegistry
import spock.lang.Specification

class ResourceLookupToolHandlerSpec extends Specification {

    void 'describe DataModel get template accepts composite query tokens'() {
        given:
        McpHttpResourceRegistry registry = new FakeRegistry()
        ResourceLookupToolHandler handler = new ResourceLookupToolHandler(registry)

        when:
        Map<String, Object> result = handler.invoke([
            resourceType: 'DataModel',
            operation: 'get',
            templatesOnly: true,
            query: 'DataModel get',
            max: 20
        ] as Map<String, Object>)

        then:
        result.count == 1
        result.operations.first().name == 'DataModel.show'
        result.operations.first().path == '/api/dataModels/{id}'
        result.operations.first().uri == 'mauro-api://http-get/api/dataModels/{id}'
    }

    static class FakeRegistry extends McpHttpResourceRegistry {

        FakeRegistry() {
            super(null)
        }

        @Override
        List<McpHttpResourceRegistry.McpHttpOperation> listOperations(String resourceType, String operationKind = null) {
            if (resourceType == 'DataModel' && operationKind == 'get') {
                return [
                new McpHttpResourceRegistry.McpHttpOperation(
                    resourceType: 'DataModel',
                    operationKind: 'get',
                    name: 'DataModel.show',
                    httpMethod: 'GET',
                    path: '/api/dataModels/{id}',
                    operationUri: 'mauro-api://operation/get/api/dataModels/{id}',
                    description: 'Returns a data model.',
                    summary: 'Get a data model',
                    operationId: 'showDataModel',
                    template: true,
                    pathParameters: ['id']
                )
            ]
            }
            []
        }

        @Override
        List<McpHttpResourceRegistry.McpHttpOperation> listOperations() {
            []
        }
    }
}
