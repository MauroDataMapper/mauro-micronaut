package org.maurodata.service.chat.mcp

import spock.lang.Specification

class ResourceReadToolHandlerSpec extends Specification {

    void 'model text treats successful resource content as authoritative data'() {
        given:
        ResourceReadToolHandler handler = new ResourceReadToolHandler(null, null)

        when:
        String text = handler.modelText([
            uri       : 'mauro-api://http-get/api/dataModels/dm-1',
            path      : '/api/dataModels/dm-1',
            name      : 'DataModel.show',
            mimeType  : 'application/json',
            statusCode: 200,
            content   : '{"id":"dm-1","label":"Braden Risk Assessment"}'
        ] as Map<String, Object>)

        then:
        text.contains('Tool mauro_get completed with HTTP 200')
        !text.contains('## Interpretation')
        text.contains('summarise the relevant fields and any available interpretations')
        !text.contains('If the HTTP status is not 2xx')
    }

    void 'model text decides failed HTTP status in backend guidance'() {
        given:
        ResourceReadToolHandler handler = new ResourceReadToolHandler(null, null)

        when:
        String text = handler.modelText([
            uri       : 'mauro-api://http-get/api/dataModels/missing',
            path      : '/api/dataModels/missing',
            name      : 'DataModel.show',
            mimeType  : 'application/json',
            statusCode: 404,
            content   : '{"message":"not found"}'
        ] as Map<String, Object>)

        then:
        text.contains('Tool mauro_get completed with HTTP 404')
        text.contains('The backend HTTP status is 404, so this resource read did not succeed.')
        text.contains('Explain that the resource could not be read and include HTTP status 404.')
        text.contains('Do not interpret the returned body as successful resource content.')
        !text.contains('If the HTTP status is not 2xx')
    }
}
