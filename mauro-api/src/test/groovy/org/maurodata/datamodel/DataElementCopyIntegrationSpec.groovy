package org.maurodata.datamodel

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_ALL)
class DataElementCopyIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID targetId

    @Shared
    DataClass targetDataClass

    @Shared
    DataClass dataClass1

    @Shared
    DataClass dataClass2

    @Shared
    DataElement dataElement1
    @Shared
    DataElement dataElement2
    @Shared
    DataType referenceTypeDataType
    @Shared
    DataType modelTypeDataType
    @Shared
    DataType primitiveDataType

    void setupSpec() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        targetDataClass = dataClassApi.create(targetId, dataClassPayload('target data class label'))
        Terminology terminology = terminologyApi.create(folderId, terminology())
        terminologyApi.finalise(terminology.id, finalisePayload())
        dataClass1 = dataClassApi.create(dataModelId, dataClassPayload('source label 1 '))
        dataClass2 = dataClassApi.create(dataModelId, dataClassPayload('source label 2'))

        referenceTypeDataType = dataTypeApi.create(dataModelId, referenceTypeDataTypePayload(dataClass1.id, 'datatype reference class label'))
        primitiveDataType = dataTypeApi.create(dataModelId, dataTypesPayload())
        modelTypeDataType = dataTypeApi.create(dataModelId, modelTypeDataTypePayload(terminology.id, Terminology.class.simpleName))
        dataElement1 = dataElementApi.create(dataModelId, dataClass2.id, dataElementPayload('data element label', referenceTypeDataType))
        dataElement2 = dataElementApi.create(dataModelId, dataClass2.id, dataElementPayload('data element label', modelTypeDataType))
    }

    @Unroll
    void 'test dataElement copy #testTargetModelId, #testTargetDataClassId, #testOtherModelId, #testOtherDataClassId, #testOtherDataElementId -should throw exception'() {
        when:
        dataElementApi.copyDataElement(testTargetModelId, testTargetDataClassId, testOtherModelId, testOtherDataClassId, testOtherDataElementId)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == exceptionStatus

        where:
        testTargetModelId | testTargetDataClassId | testOtherModelId | testOtherDataClassId | testOtherDataElementId | exceptionStatus
        targetId          | dataClass2.id         | dataModelId      | dataClass2.id        | dataElement1.id         | HttpStatus.BAD_REQUEST
        targetId          | targetDataClass.id    | dataModelId      | targetDataClass.id   | dataElement1.id         | HttpStatus.BAD_REQUEST
        targetId          | targetDataClass.id    | dataModelId      | dataClass1.id        | dataElement1.id         | HttpStatus.BAD_REQUEST
        targetId          | targetDataClass.id    | dataModelId      | dataClass2.id        | dataElement1.id         | HttpStatus.INTERNAL_SERVER_ERROR
        targetId          | targetDataClass.id    | dataModelId      | dataClass2.id        | dataElement2.id         | HttpStatus.INTERNAL_SERVER_ERROR
    }


    void 'test copy data element from another model -should copy dataElement and create new dataType in target model'() {
        given:
        ListResponse<DataType> targetModelDataTypes = dataTypeApi.list(targetId)
        targetModelDataTypes.items.isEmpty()
        and:
        DataElement created = dataElementApi.create(dataModelId, dataClass2.id, dataElementPayload('data element label', primitiveDataType))

        when:
        DataElement copied = dataElementApi.copyDataElement(targetId, targetDataClass.id, dataModelId, dataClass2.id, created.id)

        then:
        copied
        copied.id != created.id
        copied.label == created.label

        when:
        targetModelDataTypes = dataTypeApi.list(targetId)
        then:
        targetModelDataTypes.items.size() == 1
        targetModelDataTypes.items[0].id != primitiveDataType.id
        targetModelDataTypes.items[0].label == primitiveDataType.label
    }
}