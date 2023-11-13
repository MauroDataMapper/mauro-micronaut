package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared

@MicronautTest
class DataModelIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataTypeId1

    @Shared
    UUID dataTypeId2

    void 'test data model'() {
        given:
        def response = POST('/folders', [label: 'Test folder'])
        folderId = UUID.fromString(response.id)

        when:
        response = POST("/folders/$folderId/dataModels", [label: 'Test data model'])
        dataModelId = UUID.fromString(response.id)

        then:
        response
        response.label == 'Test data model'
        response.path == 'dm:Test data model$main'
    }

    void 'test data types'() {
        when:
        def response = POST("/dataModels/$dataModelId/dataTypes", [label: 'string', description: 'character string of variable length'])
        dataTypeId1 = UUID.fromString(response.id)

        then:
        response.label == 'string'

        when:
        response = POST("/dataModels/$dataModelId/dataTypes", [label: 'integer', description: 'a whole number, may be positive or negative, with no maximum or minimum'])
        dataTypeId2 = UUID.fromString(response.id)

        then:
        response.label == 'integer'

        when:
        response = GET("/dataModels/$dataModelId/dataTypes")

        then:
        response
        response.count == 2
        response.items.path.sort() == ['dm:Test data model$main|dt:integer', 'dm:Test data model$main|dt:string']
    }


}
