package org.maurodata.datamodel

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.comparison.ComparisonConclusion
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
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
    private static final String DATATYPE_LABEL = 'test modelType dataType label'
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
                         modelResourceDomainType: CodeSet.simpleName,
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
                         modelResourceDomainType: CodeSet.simpleName,
                         modelResourceId:  (finalised as Model).id))

        then:
        created
        created.domainType == DataType.DataTypeKind.MODEL_TYPE.stringValue
        created.label == DATATYPE_LABEL
        created.modelResourceDomainType == CodeSet.simpleName
        created.modelResourceId == (finalised as Model).id

        when:
        DataType retrieved = dataTypeApi.show(dataModelId, created.id)
        then:
        retrieved
        retrieved.modelResourceId == codeSet.id
        retrieved.modelResourceDomainType == CodeSet.simpleName

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
        'ModelType'     | DataClass.simpleName | dataClassId1      | HttpStatus.UNPROCESSABLE_ENTITY
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

    void 'compare primitive dataTypes syntactically reports label difference'() {
        given:
        DataType stringType = dataTypeApi.create(dataModelId, new DataType(label: 'String',
                                                                           description: 'String primitive',
                                                                           dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                           units: 'characters'))
        DataType varcharType = dataTypeApi.create(dataModelId, new DataType(label: 'Varchar',
                                                                            description: 'Variable character primitive',
                                                                            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                            units: 'characters'))

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(stringType.id, varcharType.id)

        then:
        response.count == 4

        ComparisonResult kindComparison = response.items.find {it.comparisonType == 'dataTypeKind'}
        kindComparison
        kindComparison.conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
        kindComparison.left == DataType.DataTypeKind.PRIMITIVE_TYPE.stringValue
        kindComparison.right == DataType.DataTypeKind.PRIMITIVE_TYPE.stringValue

        ComparisonResult labelComparison = response.items.find {it.comparisonType == 'label'}
        labelComparison
        labelComparison.conclusion == ComparisonConclusion.STRUCTURALLY_DIFFERENT
        labelComparison.left == 'String'
        labelComparison.right == 'Varchar'

        ComparisonResult labelTokensComparison = response.items.find {it.comparisonType == 'labelTokens'}
        labelTokensComparison
        labelTokensComparison.conclusion == ComparisonConclusion.SETS_DISJOINT
        labelTokensComparison.metadata.jaccardSimilarity == 0

        ComparisonResult unitsComparison = response.items.find {it.comparisonType == 'units'}
        unitsComparison
        unitsComparison.conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
        unitsComparison.metadata.compatible
        !unitsComparison.metadata.convertible
    }

    void 'compare primitive dataTypes can produce yaml when requested by accept header'() {
        given:
        DataType stringType = dataTypeApi.create(dataModelId, new DataType(label: 'String',
                                                                           description: 'String primitive',
                                                                           dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                           units: 'characters'))
        DataType varcharType = dataTypeApi.create(dataModelId, new DataType(label: 'Varchar',
                                                                            description: 'Variable character primitive',
                                                                            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                            units: 'characters'))
        URI uri = UriBuilder.of(embeddedServer.contextURI)
            .path("/api/dataTypes/${stringType.id}/compare/${varcharType.id}")
            .build()

        when:
        HttpResponse<String> response = httpClient.toBlocking().exchange(
            HttpRequest.GET(uri).accept(MediaType.APPLICATION_YAML),
            String
        )

        then:
        response.contentType.get().name == MediaType.APPLICATION_YAML
        response.body().contains('count:')
        response.body().contains('comparisonType:')
        response.body().contains('label')
        response.body().contains('String')
        response.body().contains('Varchar')
    }

    void 'compare primitive dataTypes reports convertible units'() {
        given:
        DataType centimetresType = dataTypeApi.create(dataModelId, new DataType(label: 'Length in centimetres',
                                                                                dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                                units: 'cm'))
        DataType metresType = dataTypeApi.create(dataModelId, new DataType(label: 'Length in metres',
                                                                           dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
                                                                           units: 'm'))

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(centimetresType.id, metresType.id)

        then:
        ComparisonResult unitsComparison = response.items.find {it.comparisonType == 'units'}
        unitsComparison
        unitsComparison.conclusion == ComparisonConclusion.STRUCTURALLY_OVERLAPPING
        unitsComparison.left == 'cm'
        unitsComparison.right == 'm'
        unitsComparison.metadata.compatible
        unitsComparison.metadata.convertible
        unitsComparison.metadata.leftDimension == 'length'
        unitsComparison.metadata.rightDimension == 'length'
        unitsComparison.metadata.conversion == 'cm = m * 100'
    }

    void 'compare enumeration dataTypes reports literal value subset direction'() {
        given:
        DataType leftEnumeration = dataTypeApi.create(dataModelId, new DataType(label: 'left enumeration',
                                                                                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        enumerationValueApi.create(dataModelId, leftEnumeration.id, new EnumerationValue(key: 'A', value: 'Alpha'))
        enumerationValueApi.create(dataModelId, leftEnumeration.id, new EnumerationValue(key: 'B', value: 'Bravo'))

        DataType rightEnumeration = dataTypeApi.create(dataModelId, new DataType(label: 'right enumeration',
                                                                                 dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        enumerationValueApi.create(dataModelId, rightEnumeration.id, new EnumerationValue(key: 'A', value: 'Alpha'))
        enumerationValueApi.create(dataModelId, rightEnumeration.id, new EnumerationValue(key: 'B', value: 'Bravo'))
        enumerationValueApi.create(dataModelId, rightEnumeration.id, new EnumerationValue(key: 'C', value: 'Charlie'))

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.LEFT_IS_SUBSET_OF_RIGHT
        declaredValueSetComparison.metadata.jaccardSimilarity == 2 / 3
        declaredValueSetComparison.metadata.shared == ['A', 'B']
        declaredValueSetComparison.metadata.leftOnlyCount == 0
        declaredValueSetComparison.metadata.rightOnly == ['C']
        declaredValueSetComparison.metadata.rightOnlyCount == 1

        ComparisonResult valueComparison = response.items.find {it.comparisonType == 'declaredValueValues'}
        valueComparison
        valueComparison.conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
        valueComparison.metadata.comparedCodesCount == 2
        valueComparison.metadata.changedCount == 0
    }

    void 'compare model resource dataTypes reports terminology and code set overlap'() {
        given:
        Terminology terminology = terminologyApi.create(folderId, new Terminology(label: 'comparison terminology'))
        Term termA = termApi.create(terminology.id, new Term(code: 'A', definition: 'Alpha'))
        Term termB = termApi.create(terminology.id, new Term(code: 'B', definition: 'Bravo'))
        Term termC = termApi.create(terminology.id, new Term(code: 'C', definition: 'Charlie'))
        Terminology finalisedTerminology = terminologyApi.finalise(terminology.id, finalisePayload())

        CodeSet codeSet = codeSetApi.create(folderId, new CodeSet(label: 'comparison code set', terms: [termB, termC] as Set<Term>))
        CodeSet finalisedCodeSet = codeSetApi.finalise(codeSet.id, finalisePayload())

        DataType terminologyType = dataTypeApi.create(dataModelId, new DataType(label: 'terminology model type',
                                                                                dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                                                                                modelResourceDomainType: Terminology.simpleName,
                                                                                modelResourceId: finalisedTerminology.id))
        DataType codeSetType = dataTypeApi.create(dataModelId, new DataType(label: 'code set model type',
                                                                            dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                                                                            modelResourceDomainType: CodeSet.simpleName,
                                                                            modelResourceId: finalisedCodeSet.id))

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(terminologyType.id, codeSetType.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.RIGHT_IS_SUBSET_OF_LEFT
        declaredValueSetComparison.metadata.shared == ['B', 'C']
        declaredValueSetComparison.metadata.leftOnly == ['A']
        declaredValueSetComparison.metadata.leftOnlyCount == 1
        declaredValueSetComparison.metadata.rightOnlyCount == 0
        declaredValueSetComparison.metadata.leftSource == 'left:Terminology'
        declaredValueSetComparison.metadata.rightSource == 'right:CodeSet'
    }

    void 'compare dataTypes reports incompatible dataTypeKind'() {
        given:
        DataType primitiveType = dataTypeApi.create(dataModelId, new DataType(label: 'String',
                                                                              dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataType enumerationType = dataTypeApi.create(dataModelId, new DataType(label: 'enumeration',
                                                                                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(primitiveType.id, enumerationType.id)

        then:
        ComparisonResult kindComparison = response.items.find {it.comparisonType == 'dataTypeKind'}
        kindComparison
        kindComparison.conclusion == ComparisonConclusion.STRUCTURALLY_INCOMPATIBLE
        kindComparison.left == DataType.DataTypeKind.PRIMITIVE_TYPE.stringValue
        kindComparison.right == DataType.DataTypeKind.ENUMERATION_TYPE.stringValue
    }

    void 'compare enumeration dataTypes reports equal literal value sets'() {
        given:
        DataType leftEnumeration = createEnumerationDataType('left equal enumeration', ['A': 'Alpha', 'B': 'Bravo'])
        DataType rightEnumeration = createEnumerationDataType('right equal enumeration', ['A': 'Alpha', 'B': 'Bravo'])

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.SETS_EQUAL
        declaredValueSetComparison.metadata.leftCount == 2
        declaredValueSetComparison.metadata.rightCount == 2
        declaredValueSetComparison.metadata.sharedCount == 2
        declaredValueSetComparison.metadata.leftOnlyCount == 0
        declaredValueSetComparison.metadata.rightOnlyCount == 0
    }

    void 'compare enumeration dataTypes reports right literal value set as subset of left'() {
        given:
        DataType leftEnumeration = createEnumerationDataType('left larger enumeration', ['A': 'Alpha', 'B': 'Bravo', 'C': 'Charlie'])
        DataType rightEnumeration = createEnumerationDataType('right smaller enumeration', ['A': 'Alpha', 'C': 'Charlie'])

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.RIGHT_IS_SUBSET_OF_LEFT
        declaredValueSetComparison.metadata.shared == ['A', 'C']
        declaredValueSetComparison.metadata.leftOnly == ['B']
        declaredValueSetComparison.metadata.rightOnlyCount == 0
    }

    void 'compare enumeration dataTypes reports overlapping literal value sets'() {
        given:
        DataType leftEnumeration = createEnumerationDataType('left overlapping enumeration', ['A': 'Alpha', 'B': 'Bravo'])
        DataType rightEnumeration = createEnumerationDataType('right overlapping enumeration', ['B': 'Bravo', 'C': 'Charlie'])

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.SETS_OVERLAP
        declaredValueSetComparison.metadata.shared == ['B']
        declaredValueSetComparison.metadata.leftOnly == ['A']
        declaredValueSetComparison.metadata.rightOnly == ['C']
    }

    void 'compare enumeration dataTypes reports disjoint literal value sets'() {
        given:
        DataType leftEnumeration = createEnumerationDataType('left disjoint enumeration', ['A': 'Alpha'])
        DataType rightEnumeration = createEnumerationDataType('right disjoint enumeration', ['B': 'Bravo'])

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult declaredValueSetComparison = response.items.find {it.comparisonType == 'declaredValueSet'}
        declaredValueSetComparison
        declaredValueSetComparison.conclusion == ComparisonConclusion.SETS_DISJOINT
        declaredValueSetComparison.metadata.sharedCount == 0
        declaredValueSetComparison.metadata.jaccardSimilarity == 0
        declaredValueSetComparison.metadata.leftOnly == ['A']
        declaredValueSetComparison.metadata.rightOnly == ['B']

        ComparisonResult valueComparison = response.items.find {it.comparisonType == 'declaredValueValues'}
        valueComparison
        valueComparison.conclusion == ComparisonConclusion.NOT_COMPARABLE_BY_THIS_PROVIDER
        valueComparison.metadata.reason == 'noSharedCodes'
        valueComparison.metadata.comparedCodesCount == 0
    }

    void 'compare enumeration dataTypes reports different values for shared literal codes'() {
        given:
        DataType leftEnumeration = createEnumerationDataType('left value enumeration', ['A': 'Alpha'])
        DataType rightEnumeration = createEnumerationDataType('right value enumeration', ['A': 'Aleph'])

        when:
        ListResponse<ComparisonResult> response = dataTypeApi.compare(leftEnumeration.id, rightEnumeration.id)

        then:
        ComparisonResult valueComparison = response.items.find {it.comparisonType == 'declaredValueValues'}
        valueComparison
        valueComparison.conclusion == ComparisonConclusion.STRUCTURALLY_DIFFERENT
        valueComparison.metadata.changed.first().code == 'A'
        valueComparison.metadata.changed.first().left == 'Alpha'
        valueComparison.metadata.changed.first().right == 'Aleph'
        valueComparison.metadata.changedCount == 1
    }

    private DataType createEnumerationDataType(String label, Map<String, String> values) {
        DataType enumerationType = dataTypeApi.create(dataModelId, new DataType(label: label,
                                                                                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        values.each {String key, String value ->
            enumerationValueApi.create(dataModelId, enumerationType.id, new EnumerationValue(key: key, value: value))
        }
        enumerationType
    }

}
