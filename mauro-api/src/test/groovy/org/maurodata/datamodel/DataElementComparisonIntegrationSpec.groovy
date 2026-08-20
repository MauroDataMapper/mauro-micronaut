package org.maurodata.datamodel

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.uri.UriBuilder
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.comparison.ComparisonConclusion
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataElementComparisonIntegrationSpec extends CommonDataSpec {

    @Inject
    @Client('/')
    HttpClient client

    UUID folderId
    UUID dataModelId
    UUID dataClassId
    DataType dataType

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Test folder')).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload('comparison data model')).id
        dataClassId = dataClassApi.create(dataModelId, dataClassPayload('comparison data class')).id
        dataType = dataTypeApi.create(dataModelId, dataTypesPayload('Comparable primitive', DataType.DataTypeKind.PRIMITIVE_TYPE))
    }

    void 'compare dataElements reports identical cardinality'() {
        given:
        DataElement left = createDataElement('left identical', dataType, 1, 1)
        DataElement right = createDataElement('right identical', dataType, 1, 1)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'label').conclusion == ComparisonConclusion.STRUCTURALLY_DIFFERENT
        ComparisonResult labelTokens = comparison(response, 'labelTokens')
        labelTokens.conclusion == ComparisonConclusion.SETS_OVERLAP
        labelTokens.metadata.shared == ['identical']
        labelTokens.metadata.jaccardSimilarity == 1 / 3
        comparison(response, 'minMultiplicity').conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
        comparison(response, 'maxMultiplicity').conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
        comparison(response, 'cardinality').conclusion == ComparisonConclusion.STRUCTURALLY_IDENTICAL
    }

    void 'compare dataElements can produce yaml when requested by accept header'() {
        given:
        DataElement left = createDataElement('left yaml', dataType, 1, 1)
        DataElement right = createDataElement('right yaml', dataType, 1, 1)
        URI uri = UriBuilder.of("/api/dataElements/${left.id}/compare/${right.id}")
            .build()

        when:
        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET(uri).accept(MediaType.APPLICATION_YAML),
            String
        )

        then:
        response.contentType.get().name == MediaType.APPLICATION_YAML
        response.body().contains('count:')
        response.body().contains('comparisonType:')
        response.body().contains('label')
        response.body().contains('left yaml')
        response.body().contains('right yaml')
    }

    void 'compare dataElements reports left cardinality narrows right'() {
        given:
        DataElement left = createDataElement('left narrow', dataType, 1, 2)
        DataElement right = createDataElement('right wide', dataType, 0, 5)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        ComparisonResult cardinality = comparison(response, 'cardinality')
        cardinality.conclusion == ComparisonConclusion.LEFT_STRUCTURALLY_NARROWS_RIGHT
        cardinality.left == [minMultiplicity: 1, maxMultiplicity: 2]
        cardinality.right == [minMultiplicity: 0, maxMultiplicity: 5]
    }

    void 'compare dataElements reports right cardinality narrows left'() {
        given:
        DataElement left = createDataElement('left wide', dataType, 0, 5)
        DataElement right = createDataElement('right narrow', dataType, 1, 2)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'cardinality').conclusion == ComparisonConclusion.RIGHT_STRUCTURALLY_NARROWS_LEFT
    }

    void 'compare dataElements reports overlapping cardinality'() {
        given:
        DataElement left = createDataElement('left overlap', dataType, 0, 2)
        DataElement right = createDataElement('right overlap', dataType, 1, 4)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'cardinality').conclusion == ComparisonConclusion.STRUCTURALLY_OVERLAPPING
    }

    void 'compare dataElements reports disjoint cardinality'() {
        given:
        DataElement left = createDataElement('left disjoint', dataType, 0, 1)
        DataElement right = createDataElement('right disjoint', dataType, 2, 4)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'cardinality').conclusion == ComparisonConclusion.STRUCTURALLY_DISJOINT
    }

    void 'compare dataElements reports incompatible cardinality when declared range is invalid'() {
        given:
        DataElement left = createDataElement('left invalid', dataType, 3, 1)
        DataElement right = createDataElement('right valid', dataType, 0, 4)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'cardinality').conclusion == ComparisonConclusion.STRUCTURALLY_INCOMPATIBLE
    }

    void 'compare dataElements reports unspecified cardinality as not comparable by syntactic provider'() {
        given:
        DataElement left = createDataElement('left unspecified', dataType, null, 1)
        DataElement right = createDataElement('right specified', dataType, 0, 1)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        comparison(response, 'minMultiplicity').conclusion == ComparisonConclusion.STRUCTURALLY_DIFFERENT

        ComparisonResult cardinality = comparison(response, 'cardinality')
        cardinality.conclusion == ComparisonConclusion.NOT_COMPARABLE_BY_THIS_PROVIDER
        cardinality.metadata.reason == 'unspecifiedMultiplicity'
    }

    void 'compare dataElements includes nested dataType comparison results'() {
        given:
        DataType leftType = dataTypeApi.create(dataModelId, dataTypesPayload('String', DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataType rightType = dataTypeApi.create(dataModelId, dataTypesPayload('Varchar', DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataElement left = createDataElement('left nested', leftType, 0, 1)
        DataElement right = createDataElement('right nested', rightType, 0, 1)

        when:
        ListResponse<ComparisonResult> response = dataElementApi.compare(left.id, right.id)

        then:
        ComparisonResult label = comparison(response, 'dataType.label')
        label.conclusion == ComparisonConclusion.STRUCTURALLY_DIFFERENT
        label.left == 'String'
        label.right == 'Varchar'
        label.metadata.nestedComparison == 'dataType'
        label.metadata.leftDataTypeId == leftType.id.toString()
        label.metadata.rightDataTypeId == rightType.id.toString()
    }

    private DataElement createDataElement(String label, DataType dataType, Integer minMultiplicity, Integer maxMultiplicity) {
        dataElementApi.create(dataModelId, dataClassId, new DataElement(
            label: label,
            description: 'comparison data element',
            dataType: new DataType(id: dataType.id),
            minMultiplicity: minMultiplicity,
            maxMultiplicity: maxMultiplicity
        ))
    }

    private ComparisonResult comparison(ListResponse<ComparisonResult> response, String comparisonType) {
        ComparisonResult result = response.items.find {it.comparisonType == comparisonType}
        assert result
        result
    }
}
