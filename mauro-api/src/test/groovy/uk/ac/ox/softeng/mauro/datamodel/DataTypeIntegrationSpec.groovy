package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql"], phase = Sql.Phase.AFTER_EACH)
class DataTypeIntegrationSpec extends CommonDataSpec {
    static String DATATYPE_LABEL = 'test modelType dataType label'
    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId1

    @Shared
    UUID dataClassId2

    @Shared
    UUID otherDataModelId

    void setupSpec() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
    }

    void setup() {
        dataModelId = dataModelApi.create(folderId, dataModelPayload('data model label')).id
        otherDataModelId = dataModelApi.create(folderId, dataModelPayload('data model label')).id
        dataClassId1 = dataClassApi.create(dataModelId, dataClassPayload('data class 1 label')).id
        dataClassId2 = dataClassApi.create(dataModelId, dataClassPayload('data class 2 label')).id
    }

    void 'create  dataType with  DataTypeKind referenceType -payload  #referenceId fails validation -should throw Unprocessible entity exception'() {
        when:
        dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: referenceId]))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        referenceId       | _
        UUID.randomUUID() | _
        folderId          | _
    }

    void 'create dataType with DataTypeKind referenceType - referenceClass in different model -should failvalidation -should throw Unprocessible entity exception'() {
        when:
        dataTypeApi.create(
            otherDataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }


    void 'create dataType with DataTypeKind referenceType -should create and show referenceClass'() {
        given:
        DataClass dataClass1 = dataClassApi.show(dataModelId, dataClassId1)

        when:
        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId,new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))

        then:
        dataTypeResponse.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue
        dataTypeResponse.label == 'test Reference Type'
        dataTypeResponse.referenceClass
        dataTypeResponse.referenceClass.id == dataClassId1
        dataTypeResponse.referenceClass.domainType == DataClass.simpleName

        dataTypeResponse.referenceClass == dataClass1
    }

    void 'create dataType with DataTypeKind referenceType - should throw UNPROCESSIBLE_ENTITY exception - when label exists in model'() {
        given:
        dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))
        and:
        ListResponse<DataType> dataTypeResponse = dataTypeApi.list(dataModelId)
        dataTypeResponse.count == 1

        when:
        dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId2]))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'create another dataType with DataTypeKind referenceType in same model - different label - should create'() {
        given:
        ListResponse<DataType> dataTypeResponse = dataTypeApi.list(dataModelId)
        dataTypeResponse.count == 0

        dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))
        and:
        dataTypeResponse = dataTypeApi.list(dataModelId)
        dataTypeResponse.count == 1

        and:
        DataClass dataClass2 = dataClassApi.show(dataModelId, dataClassId2)

        when:
        DataType dataType2 = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type 2',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId2]))

        then:
        dataType2
        dataType2.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue
        dataType2.label == 'test Reference Type 2'
        dataType2.referenceClass
        dataType2.referenceClass.id == dataClass2.id
        dataType2.referenceClass.domainType == DataClass.simpleName

        dataType2.referenceClass == dataClass2
    }

    void 'create dataType with DataTypeKind modelType and finalised- should create'() {
        given:
        CodeSet codeSet = codeSetApi.create(folderId, codeSet())

        when:
        dataTypeApi.create(
            dataModelId,
            new DataType(label: DATATYPE_LABEL,
                         description: 'Test model type description',
                         dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                         domainType: 'ModelType',
                         modelResourceDomainType: CodeSet.class.simpleName,
                         modelResourceId:  (codeSet as Model).id))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST

        CodeSet finalised = codeSetApi.finalise(codeSet.id, finalisePayload())

        when:
        DataType created = dataTypeApi.create(
            dataModelId,
            new DataType(label: DATATYPE_LABEL,
                         description: 'Test model type description',
                         dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                         domainType: 'ModelType',
                         modelResourceDomainType: CodeSet.class.simpleName,
                         modelResourceId:  (finalised as Model).id))

        then:
        created
        created.domainType == DataType.DataTypeKind.MODEL_TYPE.stringValue
        created.label == DATATYPE_LABEL
        created.modelResourceDomainType == CodeSet.class.simpleName
        created.modelResourceId == (finalised as Model).id

        when:
        DataType retrieved = dataTypeApi.show(dataModelId, created.id)
        then:
        retrieved
        retrieved.modelResourceId == codeSet.id
        retrieved.modelResourceDomainType == CodeSet.class.simpleName

        when:
        HttpResponse response = dataTypeApi.delete(dataModelId, retrieved.id, retrieved)
        then:
        response.status() == HttpStatus.NO_CONTENT
    }

    @Unroll
    void 'create dataType for #domainType, #modelResourceDomainType, #modelResourceId -should throw #expectedException'() {
        when:
        dataTypeApi.create(
            dataModelId,
            new DataType(label: DATATYPE_LABEL,
                         description: 'Test model type description',
                         dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                         domainType: domainType,
                         modelResourceDomainType: modelResourceDomainType,
                         modelResourceId: modelResourceId))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == expectedException

        where:
        domainType      | modelResourceDomainType    | modelResourceId   | expectedException
        'ModelType'     | DataClass.class.simpleName | dataClassId1      | HttpStatus.BAD_REQUEST
        'ModelType'     | _                          | UUID.randomUUID() | HttpStatus.BAD_REQUEST
        'ReferenceType' | _                          | UUID.randomUUID() | HttpStatus.BAD_REQUEST
    }


    void 'delete dataClass - should raise error when datatype is referenced in other objects'() {
        given:
        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))

        DataElement dataElement = dataElementApi.create(dataModelId, dataClassId1, dataElementPayload("dataElement label", dataTypeResponse))
        when:
        DataClass dataClass1 = dataClassApi.show(dataModelId, dataClassId1)
        then:
        dataClass1

        when:
        dataClassApi.delete(dataModelId, dataClassId1, dataClass1)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        when:
        dataClass1 = dataClassApi.show(dataModelId, dataClassId1)
        then:
        dataClass1

    }

    void 'should delete dataType when no other references to dataType'() {
        given:
        dataTypeApi.list(dataModelId).count == 0

        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))

        DataType retrieved = dataTypeApi.show(dataModelId, dataTypeResponse.id)

        when:
        HttpResponse httpResponse = dataTypeApi.delete(dataModelId, retrieved.id, retrieved)
        then:
        httpResponse.status == HttpStatus.NO_CONTENT
    }

//    Object createAPI(UUID dataModelId, UUID referenceClassId) {
//
//        dataTypeApi.create(
//            dataModelId,
//            new DataType(label: 'test Reference Type',
//                         description: 'Test Reference type description',
//                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
//                         referenceClass: [id: referenceClassId]))
//    }
}