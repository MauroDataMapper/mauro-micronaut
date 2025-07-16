package org.maurodata.datamodel


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
import org.maurodata.web.PaginationParams
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassCopyIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID targetId

    @Shared
    DataClass dataClass

    @Shared
    DataClass childDataClass

    @Shared
    DataElement dataElement

    @Shared
    DataElement childDataElement

    @Shared
    DataType referenceTypeDataType
    @Shared
    DataType modelTypeDataType
    @Shared
    DataType primitiveDataType

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload('source label')).id
        targetId = dataModelApi.create(folderId, dataModelPayload('target label')).id
        Terminology terminology = terminologyApi.create(folderId, terminology())
        terminologyApi.finalise(terminology.id, finalisePayload())
        dataClass = dataClassApi.create(dataModelId, dataClassPayload('source label'))
        referenceTypeDataType = dataTypeApi.create(dataModelId, referenceTypeDataTypePayload(dataClass.id, 'datatype reference class label'))
        modelTypeDataType = dataTypeApi.create(dataModelId, modelTypeDataTypePayload(terminology.id, Terminology.class.simpleName))
        primitiveDataType = dataTypeApi.create(dataModelId, dataTypesPayload())
        childDataClass = dataClassApi.create(dataModelId, dataClass.id, dataClassPayload('child data class label'))
    }

    void 'test copy simple dataclass with ReferenceClass dataType, has dataElement -primitiveDataType - both dataTypes created in target'() {
        given:
        ListResponse<DataType> targetDataTypes = dataTypeApi.list(targetId, new PaginationParams(), null)
        targetDataTypes.items.isEmpty()

        and:
        dataElement = dataElementApi.create(dataModelId, dataClass.id, dataElementPayload("data element label", primitiveDataType))


        when:
        DataClass copied = dataClassApi.copyDataClass(targetId, dataModelId, dataClass.id)
        then:
        copied

        copied.dataElements.size() == 1
        DataElement copiedDataElement = copied.dataElements[0]
        copiedDataElement.label == dataElement.label
        copiedDataElement.id != dataElement.id
        copiedDataElement.domainType == dataElement.domainType

        copiedDataElement.dataType.id != dataElement.dataType.id
        copiedDataElement.dataType.domainType == dataElement.dataType.domainType
        copiedDataElement.dataType.label == dataElement.dataType.label

        copied.dataClasses.size() == 1
        DataClass copiedChild = copied.dataClasses[0]
        copiedChild.label == childDataClass.label
        copiedChild.id != childDataClass.id

        copiedChild.dataElements.isEmpty()

        //referenceTypes not showing in dataclass
        when:
        ListResponse<DataType> dataTypes = dataTypeApi.list(targetId, new PaginationParams(), null)
        then:
        dataTypes
        dataTypes.items.size() == 2
        DataType copiedReferenceType  = dataTypes.items.find {it.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue}
        copiedReferenceType.id != referenceTypeDataType.id
        copiedReferenceType.referenceClass.id == copied.id
        copiedReferenceType.label == referenceTypeDataType.label


        DataType copiedDataType  = dataTypes.items.find {it.domainType == DataType.DataTypeKind.PRIMITIVE_TYPE.stringValue}
        copiedDataType.id != primitiveDataType.id
        copiedDataType.label == primitiveDataType.label

    }

    void 'test copy dataclass with child and child data element with modelType DataType'() {
        given:
        ListResponse<DataType> dataTypesBefore = dataTypeApi.list(targetId, new PaginationParams(), null)
        dataTypesBefore.items.isEmpty()
        childDataElement = dataElementApi.create(dataModelId, childDataClass.id, dataElementPayload("data element label", modelTypeDataType))

        when:
        DataClass copied = dataClassApi.copyDataClass(targetId, dataModelId, dataClass.id)

        then:
        copied
        copied.dataClasses.size() == 1
        DataClass copiedChild = copied.dataClasses[0]
        copiedChild.label == childDataClass.label
        copiedChild.id != childDataClass.id
        copiedChild.dataElements.size() == 1

        DataElement copiedChildDataElement = copiedChild.dataElements[0]
        copiedChildDataElement.id != childDataElement.id
        copiedChildDataElement.label == childDataElement.label
        copiedChildDataElement.dataType
        copiedChildDataElement.dataType.id != modelTypeDataType.id
        copiedChildDataElement.dataType.domainType == DataType.DataTypeKind.MODEL_TYPE.stringValue
        copiedChildDataElement.dataType.modelResourceId == modelTypeDataType.modelResourceId

        when:
        ListResponse<DataType> dataTypes = dataTypeApi.list(targetId, new PaginationParams(), null)
        then:
        dataTypes
        dataTypes.items.size() == 2
        dataTypes.items.domainType.containsAll(List.of(DataType.DataTypeKind.MODEL_TYPE.stringValue, DataType.DataTypeKind.REFERENCE_TYPE.stringValue))
    }
}