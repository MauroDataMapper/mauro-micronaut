package uk.ac.ox.softeng.mauro.datamodel

import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-dataelement.sql", phase = Sql.Phase.AFTER_ALL)
class DataElementUpdateIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataTypeId

    @Shared
    UUID dataClassId

    @Shared
    UUID dataElementId

    @Shared
    UUID secondDataTypeId

    void setupSpec() {
        folderId = folderApi.create(folder()).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload()).id
        dataTypeId = dataTypeApi.create(dataModelId, dataTypesPayload()).id
        dataClassId = dataClassApi.create(dataModelId, dataClassPayload()).id
        dataElementId = dataElementApi.create(
                dataModelId,
                dataClassId,
                new DataElement(label: 'First data element', description: 'The first data element', dataType: new DataType(id: dataTypeId))).id
        secondDataTypeId = dataTypeApi.create(dataModelId, new DataType(label: 'second data type', dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE, units: 'lbs')).id

    }

    void 'test update data element -no datatype in update Request -should update as expected'() {
        when:
        DataElement dataElementResponse = dataElementApi.update(dataModelId, dataClassId,dataElementId, new DataElement(label: 'Renamed data element'))
        then:
        dataElementResponse
        dataElementResponse.label == 'Renamed data element'
        dataElementResponse.dataType.id == dataTypeId
    }

    void 'test update data element -new datatype has different datamodel -should throw exception'() {
        given:
        dataElementId = dataElementApi.create(
                dataModelId,
                dataClassId,
                new DataElement(label: 'First data element', description: 'The first data element', dataType: new DataType(id: dataTypeId))).id

        UUID differentDataModelId = dataModelApi.create(folderId, dataModelPayload()).id
        UUID differentDataTypeId = dataTypeApi.create(differentDataModelId, dataTypesPayload()).id
        when:
        dataElementApi.update(
                dataModelId,
                dataClassId,
                dataElementId,
                new DataElement(label: 'Renamed data element', dataType: new DataType(id: differentDataTypeId)))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

    @Unroll
    void 'test update data element #dataElementPayload  -should/should not update with expected data'() {
        when:
        DataElement updated = dataElementApi.update(dataModelId, dataClassId, dataElementId, dataElementPayload)
        then:
        updated
        updated.label == expectedLabel
        updated.dataType.id == expectedDataTypeId

        when:
        DataElement retrieved = dataElementApi.show(dataModelId, dataClassId, dataElementId)

        then:
        retrieved
        retrieved.dataType.id == expectedDataTypeId
        retrieved.label == expectedLabel

        where:
        dataElementPayload                                                                         | expectedDataTypeId | expectedLabel
        new DataElement(label: 'Renamed data element')                                             | dataTypeId         | 'Renamed data element'
        new DataElement(label: 'label data element', dataType: new DataType(id: secondDataTypeId)) | secondDataTypeId   | 'label data element'
        new DataElement(dataType: new DataType(id: dataTypeId))                                    | dataTypeId         | 'label data element'
        new DataElement(dataType: new DataType(id: secondDataTypeId))                              | secondDataTypeId   | 'label data element'
        new DataElement(label: 'data element label', dataType: new DataType(id: dataTypeId))       | dataTypeId         | 'data element label'

    }

}