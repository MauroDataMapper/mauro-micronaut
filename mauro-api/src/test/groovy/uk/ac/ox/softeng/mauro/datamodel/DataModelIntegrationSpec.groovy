package uk.ac.ox.softeng.mauro.datamodel

import io.micronaut.http.HttpStatus
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
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

    @Shared
    UUID dataElementId1

    @Shared
    UUID dataElementId2

    @Shared
    UUID enumerationValueId

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

        when:
        HttpStatus status = DELETE("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses/$dataClassId3", [label: 'Third data class (renamed)'], HttpStatus)
        dataClassListResponse = (ListResponse<DataClass>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId2/dataClasses", ListResponse<DataClass>)
        then:
        dataClassListResponse.count == 0

    }


    void 'test data elements'() {
        when:
        ListResponse<DataElement> dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements")
        then:
        dataElementListResponse.count == 0

        when:
        DataElement dataElementResponse = (DataElement) POST("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements", [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId1]], DataElement)
        dataElementId1 = dataElementResponse.id

        then:
        dataElementResponse.label == 'First data element'

        when:
        dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements")

        then:
        dataElementListResponse.count == 1

        when:
        dataElementResponse = (DataElement) POST("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements", [label: 'Second data element', description: 'The second data element', dataType: [id: dataTypeId2]], DataElement)
        dataElementId2 = dataElementResponse.id

        then:
        dataElementResponse.label == 'Second data element'

        when:
        dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements")

        then:
        dataElementListResponse.count == 2
        dataElementListResponse.items.label.sort() == ['First data element', 'Second data element']

        when:
        dataElementResponse = (DataElement) PUT("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements/$dataElementId1", [label: 'Renamed data element', description: 'The second data element', dataType: [id: dataTypeId2]], DataElement)

        then:
        dataElementResponse.label == 'Renamed data element'

        when:
        dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements")

        then:
        dataElementListResponse.count == 2
        dataElementListResponse.items.label.sort() == ['Renamed data element', 'Second data element']

        when:
        HttpStatus status = (HttpStatus) DELETE("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements/$dataElementId2", [label: ''], HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        dataElementListResponse = (ListResponse<DataElement>) GET("/dataModels/$dataModelId/dataClasses/$dataClassId1/dataElements")

        then:
        dataElementListResponse.count == 1
        dataElementListResponse.items.label.sort() == ['Renamed data element']




    }


    void 'test enumeration values'() {
        when:
        DataType dataTypeResponse = (DataType) POST("/dataModels/$dataModelId/dataTypes", [label: 'Boolean', description: 'Either true or false',domainType: 'EnumerationType'], DataType)
        UUID enumerationTypeId = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'Boolean'

        when:
        EnumerationValue enumerationValueResponse = (EnumerationValue) POST("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", [key: 'T', value: 'True'], EnumerationValue)
        enumerationValueId = enumerationValueResponse.id

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

        when:
        enumerationValueResponse = (EnumerationValue) PUT("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues/$enumerationValueId", [key: 'R', value: 'Renamed'], EnumerationValue)

        then:
        enumerationValueResponse.label == 'R'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        enumerationValueListResponse = (ListResponse<EnumerationValue>) GET("/dataModels/$dataModelId/dataTypes/$enumerationTypeId/enumerationValues", ListResponse<EnumerationValue>)

        then:
        enumerationValueListResponse.count == 2

        enumerationValueListResponse.items.find { it -> it.key == 'R' } != null
        enumerationValueListResponse.items.find { it -> it.key == 'F' } != null



    }


}
