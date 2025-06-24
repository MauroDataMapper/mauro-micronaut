package org.maurodata.datamodel


import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
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

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassDeleteIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId

    @Shared
    UUID childDataClassId1

    @Shared
    UUID childDataClassId2

    @Shared
    UUID grandChildDataClassId1

    @Shared
    UUID grandChildDataClassId2

    @Shared
    DataElement dataElement

    @Shared
    DataElement childDataElement

    @Shared
    DataType referenceTypeDataType
    @Shared
    DataType modelTypeDataType

    @Shared
    UUID terminologyId

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload('data model label')).id
        dataClassId = dataClassApi.create(dataModelId, dataClassPayload('source label')).id
        childDataClassId1 = dataClassApi.create(dataModelId, dataClassId, dataClassPayload('child data class 1 label')).id
        childDataClassId2 = dataClassApi.create(dataModelId, dataClassId, dataClassPayload('child data class 2 label')).id
        grandChildDataClassId1 = dataClassApi.create(dataModelId, childDataClassId1, dataClassPayload('grand child data class 1 label')).id
        grandChildDataClassId2 = dataClassApi.create(dataModelId, childDataClassId2, dataClassPayload('grand child data class 2 label')).id
        terminologyId = terminologyApi.create(folderId, terminology()).id
        terminologyApi.finalise(terminologyId, finalisePayload())

        referenceTypeDataType = dataTypeApi.create(dataModelId, referenceTypeDataTypePayload(grandChildDataClassId1, 'datatype reference class label grandchild DC'))
        modelTypeDataType = dataTypeApi.create(dataModelId, modelTypeDataTypePayload(terminologyId, Terminology.class.simpleName))
        childDataElement = dataElementApi.create(dataModelId, childDataClassId2, dataElementPayload("data element label childDC2", modelTypeDataType))
    }


    void 'should delete child dataClass 2 and associations'() {
        given:
        dataClassApi.list(dataModelId, childDataClassId2).items.size() > 0

        and:
        DataClass grandChild = dataClassApi.show(dataModelId, grandChildDataClassId2)
        when:
        HttpResponse response = dataClassApi.delete(dataModelId, childDataClassId2, grandChild)

        then:
        response.status() == HttpStatus.NO_CONTENT

        when:
        ListResponse<DataClass> dataClassListResponse = dataClassApi.list(dataModelId, childDataClassId2)

        then:
        dataClassListResponse.items.isEmpty()

        when:
        DataType modelReferenceDataType = dataTypeApi.show(dataModelId, modelTypeDataType.id)
        then:
        modelReferenceDataType


    }

    void 'should delete dataClass and associations'() {
        given:
        DataClass dataClass = dataClassApi.show(dataModelId, dataClassId)

        when:
        HttpResponse response = dataClassApi.delete(dataModelId, dataClassId, dataClass)

        then:
        response.status() == HttpStatus.NO_CONTENT

        when:
        ListResponse<DataClass> dataClassListResponse = dataClassApi.list(dataModelId, dataClassId)

        then:
        dataClassListResponse.items.isEmpty()

        when:
        DataType modelReferenceDataType = dataTypeApi.show(dataModelId, modelTypeDataType.id)
        then:
        modelReferenceDataType


    }


}