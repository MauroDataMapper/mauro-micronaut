package org.maurodata.datamodel

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataElementShowIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId

    @Shared
    DataType enumType

    @Shared
    EnumerationValue enumerationValue

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        dataClassId = dataClassApi.create(dataModelId, dataClassPayload('source label 1 ')).id
        enumType = dataTypeApi.create(dataModelId, enumerationValueDataTypePayload('enum value label'))
        enumerationValue = enumerationValueApi.create(dataModelId, enumType.id,
                                                      new EnumerationValue(key: 'F', value: 'False'))
    }


    void 'show dataelement -with dataType enumerationValue -should show enumeration values in response'() {
        given:
        DataElement dataElement = dataElementApi.create(dataModelId, dataClassId, dataElementPayload('dataElement label', enumType))
        when:
        DataElement response = dataElementApi.show(dataModelId, dataClassId, dataElement.id)

        then:
        response
        response.dataType
        response.dataType.enumerationValues.size() == 1
        response.dataType.enumerationValues[0].label == enumerationValue.label
    }


    void 'show dataelement -dataType is not enumerationType -should display with dataType info'() {
        given:
        DataType primitive = dataTypeApi.create(dataModelId, dataTypesPayload())
        DataElement dataElement = dataElementApi.create(dataModelId, dataClassId, dataElementPayload('dataElement label', primitive))

        when:
        DataElement response = dataElementApi.show(dataModelId, dataClassId, dataElement.id)

        then:
        response
        response.dataType
        response.dataType.dataTypeKind == primitive.dataTypeKind
    }
}



