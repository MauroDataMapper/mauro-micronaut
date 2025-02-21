package uk.ac.ox.softeng.mauro.datamodel

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
@Sql(scripts = "classpath:sql/tear-down-dataelement.sql", phase = Sql.Phase.AFTER_ALL)
class DataElementUpdateIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

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
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)).id
        dataTypeId = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)).id
        dataClassId = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)).id
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH",
                                            [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId]], DataElement)).id
        secondDataTypeId = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH",
                                            [label: 'second data type', domainType: 'primitiveType', units: 'lbs'], DataType)).id

    }

    void 'test update data element -no datatype in update Request -should update as expected'() {
        given:
        DataElement dataElementResponse = (DataElement) PUT("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH/$dataElementId",
                                                            [label: 'Renamed data element'], DataElement)
        when:
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH",
                                            [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId]], DataElement)).id
        then:
        dataElementResponse
        dataElementResponse.label == 'Renamed data element'
        dataElementResponse.dataType.id == dataTypeId
    }

    void 'test update data element -new datatype has different datamodel -should throw exception'() {
        given:
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH",
                                            [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId]], DataElement)).id

        UUID differentDataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)).id
        UUID differentDataTypeId = ((DataType) POST("$DATAMODELS_PATH/$differentDataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)).id
        when:
        PUT("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH/$dataElementId",
            [label: 'Renamed data element', dataType: [id: differentDataTypeId]], DataElement)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

    @Unroll
    void 'test update data element #dataElementPayload  -should/should not update with expected data'() {
        when:
        DataElement updated = (DataElement) PUT("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH/$dataElementId",
                                                dataElementPayload, DataElement)
        then:
        updated
        updated.label == expectedLabel
        updated.dataType.id == expectedDataTypeId

        when:
        DataElement retrieved = (DataElement) GET("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH/$dataElementId", DataElement)

        then:
        retrieved
        retrieved.dataType.id == expectedDataTypeId
        retrieved.label == expectedLabel

        where:
        dataElementPayload                                              | expectedDataTypeId | expectedLabel
        [label: 'Renamed data element']                                 | dataTypeId         | 'Renamed data element'
        [label: 'label data element', dataType: [id: secondDataTypeId]] | secondDataTypeId   | 'label data element'
        [dataType: [id: dataTypeId]]                                    | dataTypeId         | 'label data element'
        [dataType: [id: secondDataTypeId]]                              | secondDataTypeId   | 'label data element'
        [label: 'data element label', dataType: [id: dataTypeId]]       | dataTypeId         | 'data element label'

    }

}