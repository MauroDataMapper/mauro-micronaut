package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared

@ContainerizedTest
class DataModelIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    ObjectMapper objectMapper

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

    @Shared
    UUID dataTypeId3

    @Shared
    UUID dataClassId1

    @Shared
    UUID dataClassId2

    @Shared
    UUID dataClassId3

    void 'test data model'() {

        given:
        DataModel response = (DataModel) POST('/folders', [label: 'Test folder'], DataModel)
        folderId = response.id

        when:
        response = (DataModel) POST("/folders/$folderId/dataModels", [label: 'Test data model'], DataModel)
        dataModelId = response.id

        then:
        response.label == 'Test data model'
        response.path.toString() == 'dm:Test data model$main'
    }

    void 'test data types'() {
        when:
        DataType dataTypeResponse = (DataType) POST("/dataModels/$dataModelId/dataTypes", [label: 'string', description: 'character string of variable length', domainType: 'PrimitiveType'], DataType)
        dataTypeId1 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'string'

        when:
        dataTypeResponse = (DataType) POST("/dataModels/$dataModelId/dataTypes", [label: 'integer', description: 'a whole number, may be positive or negative, with no maximum or minimum', domainType: 'PrimitiveType'], DataType)
        dataTypeId2 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'integer'

        when:
        ListResponse<DataType> dataTypesListResponse = (ListResponse<DataType>) GET("/dataModels/$dataModelId/dataTypes", ListResponse<DataType>)

        then:
        dataTypesListResponse.count == 2
        dataTypesListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dt:integer', 'dm:Test data model$main|dt:string']
        dataTypesListResponse.items.domainType == ['PrimitiveType', 'PrimitiveType']

        when:
        dataTypeResponse = (DataType) POST("/dataModels/$dataModelId/dataTypes",
                        [label: 'Yes/No',
                         description: 'Either a yes or a no',
                         domainType: 'EnumerationType',
/*                         enumerationValues: [
                             [key: 'Y', value: 'Yes'],
                             [key: 'N', value: 'No']] */
                         ], DataType)
        dataTypeId3 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'Yes/No'
        dataTypeResponse.domainType == 'EnumerationType'

    }

    void 'test data classes'() {
        when:
        DataClass dataClassResponse = (DataClass) POST("/dataModels/$dataModelId/dataClasses", [label: 'First data class', description: 'The first data class'], DataClass)
        dataClassId1 = dataClassResponse.id

        then:
        dataClassResponse.label == 'First data class'

        when:
        dataClassResponse = (DataClass) POST("/dataModels/$dataModelId/dataClasses", [label: 'Second data class', description: 'The second data class'], DataClass)
        dataClassId2 = dataClassResponse.id

        then:
        dataClassResponse.label == 'Second data class'

        when:
        ListResponse<DataClass> dataClassListResponse = (ListResponse<DataClass>) GET("/dataModels/$dataModelId/dataClasses", ListResponse<DataClass>)

        then:
        dataClassListResponse.count == 2
        dataClassListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dc:First data class', 'dm:Test data model$main|dc:Second data class']

        when:
        dataClassResponse = (DataClass) POST("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses", [label: 'Third data class', description: 'The third data class'], DataClass)
        dataClassId3 = dataClassResponse.id

        then:
        dataClassResponse.label == 'Third data class'

        when:
        dataClassListResponse = (ListResponse<DataClass>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses", ListResponse<DataClass>)

        then:
        dataClassListResponse.count == 1

        // TODO: Make this work!
        // dataClassListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dc:Second data class|dc:Third data class']

        when:
        dataClassResponse = (DataClass) PUT("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses/$dataClassId3", [label: 'Third data class (renamed)'], DataClass)
        dataClassListResponse = (ListResponse<DataClass>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses", ListResponse<DataClass>)

        then:
        dataClassResponse.label == 'Third data class (renamed)'
        dataClassListResponse.count == 1
        // TODO: Make this work!
        // dataClassListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dc:Second data class|dc:Third data class (renamed)']

/*  todo
        when:
        Map response = DELETE("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses/$dataClassId3", [label: 'Third data class (renamed)'])
        System.err.println(response)
        dataClassListResponse = (ListResponse<DataClass>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses", ListResponse<DataClass>)
        then:
        dataClassListResponse.count == 0
*/


    }

    void 'test enumeration values'() {
        when:
        DataType dataTypeResponse = (DataType) POST("/dataModels/$dataModelId/dataTypes", [label: 'Boolean', description: 'Either true or false',domainType: 'EnumerationType'], DataType)
        UUID enumerationTypeId = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'Boolean'

        when:
        EnumerationValue enumerationValueResponse = (EnumerationValue) POST("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", [key: 'T', value: 'True'], EnumerationValue)

        then:
        enumerationValueResponse.label == 'T'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        enumerationValueResponse = (EnumerationValue) POST("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", [key: 'F', value: 'False'], EnumerationValue)

        then:
        enumerationValueResponse.label == 'F'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        ListResponse<EnumerationValue> enumerationValueListResponse = (ListResponse<EnumerationValue>) GET("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", ListResponse<EnumerationValue>)

        then:
        enumerationValueListResponse.count == 2

        enumerationValueListResponse.items.find { it -> it.key == 'T' } != null
        enumerationValueListResponse.items.find { it -> it.key == 'F' } != null

    }


}
