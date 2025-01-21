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

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-dataelement.sql", phase = Sql.Phase.AFTER_EACH)
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

    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)).id
        dataTypeId = ((DataType) POST("$DATAMODELS_PATH/$dataModelId$DATATYPES_PATH", dataTypesPayload(), DataType)).id
        dataClassId = ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", dataClassPayload(), DataClass)).id
        dataElementId = ((DataElement) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH",
                                            [label: 'First data element', description: 'The first data element', dataType: [id: dataTypeId]], DataElement)).id
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

    void 'test update data element -new changes to existing dataType should update'() {

        when:
        DataElement dataElement = (DataElement) PUT("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH/$dataClassId$DATA_ELEMENTS_PATH/$dataElementId",
            [label   : 'Renamed data element',
             dataType:
                 [id         : dataTypeId,
                  label      : 'changed label primitive',
                  description: 'changed datatype description'
                 ]], DataElement)
        then:
        dataElement
        dataElement.dataType
        dataElement.label == 'Renamed data element'
        dataElement.dataType.label == 'changed label primitive'
        dataElement.dataType.description == 'changed datatype description'
    }

}