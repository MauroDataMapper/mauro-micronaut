package uk.ac.ox.softeng.mauro.datamodel


import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelType
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.model.version.VersionChangeType
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.web.PaginationParams

@ContainerizedTest
@Singleton
class DataModelIntegrationSpec extends CommonDataSpec {

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
        Folder response = folderApi.create(new Folder(label: 'Test folder'))
        folderId = response.id

        when:
        DataModel dataModelResponse = dataModelApi.create(folderId, new DataModel(label: 'Test data model'))
        dataModelId = dataModelResponse.id

        then:
        dataModelResponse.label == 'Test data model'
        dataModelResponse.dataModelType == DataModelType.DATA_ASSET.label
        dataModelResponse.path.toString() == 'fo:Test folder|dm:Test data model$main'
        dataModelResponse.authority

        when:
        dataModelResponse = dataModelApi.show(dataModelId)

        then:
        dataModelResponse.label == 'Test data model'
        dataModelResponse.dataModelType == DataModelType.DATA_ASSET.label
        dataModelResponse.path.toString() == 'fo:Test folder|dm:Test data model$main'
        dataModelResponse.authority

        when:
        dataModelResponse = dataModelApi.create(folderId, new DataModel(label: 'Test data standard', dataModelType: DataModelType.DATA_STANDARD.label))
        UUID dataStandardId = dataModelResponse.id

        then:
        dataModelResponse.label == 'Test data standard'
        dataModelResponse.dataModelType == DataModelType.DATA_STANDARD.label
        dataModelResponse.path.toString() == 'fo:Test folder|dm:Test data standard$main'
        dataModelResponse.authority

        when:
        dataModelResponse = dataModelApi.show(dataStandardId)

        then:
        dataModelResponse.label == 'Test data standard'
        dataModelResponse.dataModelType == DataModelType.DATA_STANDARD.label
        dataModelResponse.path.toString() == 'fo:Test folder|dm:Test data standard$main'
        dataModelResponse.authority
    }

    void 'test finalise data model'() {

        given:
        Folder response = folderApi.create(new Folder(label: 'Test folder'))
        folderId = response.id

        dataModelId = dataModelApi.create(folderId, new DataModel(label: 'Test data model')).id

        when:
        DataModel finalised = dataModelApi.finalise(
            dataModelId,
            new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'random version tag'))

        then:
        finalised
        finalised.finalised == true
        finalised.dateFinalised
        finalised.authority
    }

    void 'test data types'() {
        when:
        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId, new DataType(
            label: 'string',
            description: 'character string of variable length',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        dataTypeId1 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'string'

        when:
        dataTypeResponse =
        dataTypeResponse = dataTypeApi.create(
            dataModelId, new DataType(
            label: 'integer',
            description: 'a whole number, may be positive or negative, with no maximum or minimum',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        dataTypeId2 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'integer'

        when:
        ListResponse<DataType> dataTypesListResponse =
            dataTypeApi.list(dataModelId)

        then:
        dataTypesListResponse.count == 2
        dataTypesListResponse.items.path.collect {it.toString()}.sort() == ['fo:Test folder|dm:Test data model$1.0.0|dt:integer', 'fo:Test folder|dm:Test data model$1.0.0|dt:string']
        dataTypesListResponse.items.domainType == ['PrimitiveType', 'PrimitiveType']

        when:
        dataTypeResponse =
            dataTypeApi.create(dataModelId, DataType.build(
                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE)
                {
                    label 'Yes/No'
                    description 'Either a yes or a no'
/*                    enumerationValue {
                        key 'Y'
                        value 'Yes'
                    }
                    enumerationValue {
                        key 'N'
                        value 'No'
                    }*/
                })
        dataTypeId3 = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'Yes/No'
        dataTypeResponse.domainType == 'EnumerationType'
    }

    void 'test data classes'() {
        when:
        DataClass dataClassResponse = dataClassApi.create(
            dataModelId,
            new DataClass(label: 'First data class', description: 'The first data class'))
        dataClassId1 = dataClassResponse.id

        then:
        dataClassResponse.label == 'First data class'

        when:
        dataClassResponse = dataClassApi.create(
            dataModelId,
            new DataClass(label: 'Second data class', description: 'The second data class'))
        dataClassId2 = dataClassResponse.id

        then:
        dataClassResponse.label == 'Second data class'

        when:
        ListResponse<DataClass> dataClassListResponse =
            dataClassApi.list(dataModelId, new PaginationParams())

        then:
        dataClassListResponse.count == 2
        dataClassListResponse.items.path.collect {it.toString()}.sort() == ['fo:Test folder|dm:Test data model$1.0.0|dc:First data class', 'fo:Test folder|dm:Test data model$1.0.0|dc:Second data class']

        when:
        dataClassResponse = dataClassApi.create(
            dataModelId,
            dataClassId2,
            new DataClass(label: 'Third data class', description: 'The third data class'))
        dataClassId3 = dataClassResponse.id

        then:
        dataClassResponse.label == 'Third data class'

        when:
        dataClassListResponse = dataClassApi.list(dataModelId, dataClassId2)

        then:
        dataClassListResponse.count == 1

        // TODO: Make this work!
        // dataClassListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dc:Second data class|dc:Third data class']

        when:
        dataClassResponse = dataClassApi.update(
            dataModelId,
            dataClassId2,
            dataClassId3,
            new DataClass(label: 'Third data class (renamed)'))
        dataClassListResponse = dataClassApi.list(dataModelId, dataClassId2)

        then:
        dataClassResponse.label == 'Third data class (renamed)'
        dataClassListResponse.count == 1
        // TODO: Make this work!
        // dataClassListResponse.items.path.sort().collect {it.toString()} == ['dm:Test data model$main|dc:Second data class|dc:Third data class (renamed)']

        when:
        HttpResponse response = dataClassApi.delete(
            dataModelId,
            dataClassId2,
            dataClassId3,
            new DataClass(label: 'Third data class (renamed)'))

        dataClassListResponse = dataClassApi.list(dataModelId, dataClassId2)
        then:
        dataClassListResponse.count == 0

    }


    void 'test data elements'() {
        when:
        ListResponse<DataElement> dataElementListResponse = dataElementApi.list(dataModelId, dataClassId1, new PaginationParams())
        then:
        dataElementListResponse.count == 0

        when:
        DataElement dataElementResponse = dataElementApi.create(
            dataModelId,
            dataClassId1,
            new DataElement(
                label: 'First data element',
                description: 'The first data element',
                dataType: new DataType(id: dataTypeId1)))
        dataElementId1 = dataElementResponse.id

        then:
        dataElementResponse.label == 'First data element'

        when:
        dataElementListResponse = dataElementApi.list(dataModelId, dataClassId1, new PaginationParams())

        then:
        dataElementListResponse.count == 1

        when:
        dataElementResponse = dataElementApi.create(
            dataModelId,
            dataClassId1,
            new DataElement(label: 'Second data element',
                            description: 'The second data element',
                            dataType: new DataType(id: dataTypeId2)))
        dataElementId2 = dataElementResponse.id

        then:
        dataElementResponse.label == 'Second data element'

        when:
        dataElementListResponse = dataElementApi.list(dataModelId, dataClassId1, new PaginationParams())

        then:
        dataElementListResponse.count == 2
        dataElementListResponse.items.label.sort() == ['First data element', 'Second data element']

        when:
        dataElementResponse = dataElementApi.update(
            dataModelId,
            dataClassId1,
            dataElementId1,
            new DataElement(label: 'Renamed data element',
                            description: 'The second data element',
                            dataType: new DataType(id: dataTypeId2)))

        then:
        dataElementResponse.label == 'Renamed data element'
        dataElementResponse.dataType.id == dataTypeId2
        Integer minMultiplicity = dataElementResponse.minMultiplicity

        when:
        dataElementResponse = dataElementApi.update(
                dataModelId,
                dataClassId1,
                dataElementId1,
                new DataElement(minMultiplicity: 5, dataType: new DataType(id: dataTypeId2)))

        then:
        dataElementResponse.label == 'Renamed data element'
        dataElementResponse.dataType.id == dataTypeId2
        dataElementResponse.minMultiplicity != minMultiplicity

        when:
        dataElementListResponse = dataElementApi.list(dataModelId, dataClassId1, new PaginationParams())

        then:
        dataElementListResponse.count == 2
        dataElementListResponse.items.label.sort() == ['Renamed data element', 'Second data element']

        when:
        HttpResponse response = dataElementApi.delete(
            dataModelId,
            dataClassId1,
            dataElementId2,
            new DataElement())

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        dataElementListResponse = dataElementApi.list(dataModelId, dataClassId1, new PaginationParams())

        then:
        dataElementListResponse.count == 1
        dataElementListResponse.items.label.sort() == ['Renamed data element']




    }


    void 'test enumeration values'() {
        when:
        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'Boolean',
                         description: 'Either true or false',
                         dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        UUID enumerationTypeId = dataTypeResponse.id

        then:
        dataTypeResponse.label == 'Boolean'

        when:
        EnumerationValue enumerationValueResponse = enumerationValueApi.create(
            dataModelId,
            enumerationTypeId,
            new EnumerationValue(key: 'T', value: 'True'))
        enumerationValueId = enumerationValueResponse.id

        then:
        enumerationValueResponse.label == 'T'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        enumerationValueResponse = enumerationValueApi.create(
            dataModelId,
            enumerationTypeId,
            new EnumerationValue(key: 'F', value: 'False'))

        then:
        enumerationValueResponse.label == 'F'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        ListResponse<EnumerationValue> enumerationValueListResponse =
            enumerationValueApi.list(dataModelId, enumerationTypeId)

        then:
        enumerationValueListResponse.count == 2

        enumerationValueListResponse.items.find { it -> it.key == 'T' }
        enumerationValueListResponse.items.find { it -> it.key == 'F' }

        when:
        enumerationValueResponse = enumerationValueApi.update(
            dataModelId,
            enumerationTypeId,
            enumerationValueId,
            new EnumerationValue(key: 'R', value: 'Renamed'))

        then:
        enumerationValueResponse.label == 'R'
        enumerationValueResponse.domainType == 'EnumerationValue'

        when:
        enumerationValueListResponse = enumerationValueApi.list(dataModelId, enumerationTypeId)

        then:
        enumerationValueListResponse.count == 2

        enumerationValueListResponse.items.find { it -> it.key == 'R' } != null
        enumerationValueListResponse.items.find { it -> it.key == 'F' } != null

    }

    void "test search within model"() {

        expect:

        ListResponse<SearchResultsDTO> searchResults =
            dataModelApi.searchGet(dataModelId,
                new SearchRequestDTO(searchTerm: searchTerm, domainTypes: domainTypes))

        searchResults.items.label == expectedLabels

        where:

        searchTerm          | domainTypes                               | expectedLabels
        "first"             | []                                        | ["First data class"]
        "second"            | []                                        | ["Second data class", "Renamed data element"]
        "second"            | ["DataClass"]                             | ["Second data class"]
        "second"            | ["DataElement"]                           | ["Renamed data element"]

    }

}
