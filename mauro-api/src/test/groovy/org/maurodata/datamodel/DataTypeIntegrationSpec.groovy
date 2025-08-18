package org.maurodata.datamodel

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
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

    @Shared
    @Inject
    HttpClient httpClient

    @Shared
    @Inject
    EmbeddedServer embeddedServer


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
        ListResponse<DataType> dataTypeResponse = dataTypeApi.list(dataModelId, new PaginationParams())
        dataTypeResponse.count == 0

        dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))
        and:
         URI uri = UriBuilder.of(embeddedServer.getContextURI())
            .path("/api/dataModels/".concat(dataModelId.toString()).concat('/dataTypes'))
            .queryParam('domainType', DataType.DataTypeKind.REFERENCE_TYPE.stringValue)
            .build()
        when:

        Map<String, Object> response = httpClient.toBlocking().retrieve(uri.toString(), Map<String, Object>)

        then:
        response
        response.get('count') == 1


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
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

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

        URI uri = UriBuilder.of(embeddedServer.getContextURI())
            .path("/api/dataModels/".concat(dataModelId.toString()).concat('/dataTypes'))
            .queryParam('domainType', DataType.DataTypeKind.MODEL_TYPE.stringValue)
            .build()
        when:

        Map<String, Object> lowLevelResponse = httpClient.toBlocking().retrieve(uri.toString(), Map<String, Object>)

        then:
        lowLevelResponse
        lowLevelResponse.get('count') == 1


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
        'ModelType'     | DataClass.class.simpleName | dataClassId1      | HttpStatus.UNPROCESSABLE_ENTITY
        'ModelType'     | _                          | UUID.randomUUID() | HttpStatus.UNPROCESSABLE_ENTITY
        'ReferenceType' | _                          | UUID.randomUUID() | HttpStatus.UNPROCESSABLE_ENTITY
    }


    void 'delete dataClass - should raise error when datatype is referenced in other objects'() {
        given:
        DataType dataTypeResponse = dataTypeApi.create(
            dataModelId,
            new DataType(label: 'test Reference Type',
                         description: 'Test Reference type description',
                         dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                         referenceClass: [id: dataClassId1]))


        dataElementApi.create(dataModelId, dataClassId2, dataElementPayload("dataElement label", dataTypeResponse))
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
        dataTypeApi.list(dataModelId, new PaginationParams()).count == 0

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

    void 'should get enumeration values associated with EnumerationType dataType'() {
        given:
        DataType enumerationType = dataTypeApi.create(dataModelId, new DataType(label: 'test enumerationType',
                                                                                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        enumerationValueApi.create(dataModelId, enumerationType.id, new EnumerationValue().tap {
            key = 'yes'
            value = 'yes value'
        })
        enumerationValueApi.create(dataModelId, enumerationType.id, new EnumerationValue().tap {
            key = 'no'
            value = 'no value'
        })


        when:
        DataType retrieved = dataTypeApi.show(dataModelId, enumerationType.id)

        then:
        retrieved
        retrieved.enumerationValues.size() == 2

        when:
        ListResponse<DataType> dataTypeListResponse = dataTypeApi.list(dataModelId,  new PaginationParams())
        then:
        dataTypeListResponse
        dataTypeListResponse.items.size() == 1
        dataTypeListResponse.items.first().enumerationValues.size() == 2

    }

}