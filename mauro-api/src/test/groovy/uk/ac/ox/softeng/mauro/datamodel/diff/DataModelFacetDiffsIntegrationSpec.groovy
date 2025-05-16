package uk.ac.ox.softeng.mauro.datamodel.diff


import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.FieldDiff
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.model.version.VersionChangeType
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelFacetDiffsIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId
    @Shared
    DataModel left

    @Shared
    UUID metadataId

    @Shared
    UUID annotationId

    void setup() {
        Folder response = folderApi.create(folder())
        folderId = response.id

        left = dataModelApi.create(folderId, dataModelPayload())

        Metadata metadataResponse = metadataApi.create("DataModel", left.id, metadataPayload())
        metadataId = metadataResponse.id

        Annotation annotationResponse = annotationApi.create("DataModel", left.id, annotationPayload())
        annotationId = annotationResponse.id
    }

    void 'test datamodel diff - lhs has facets - diff should have deleted items only'() {
        given:
        metadataApi.create("DataModel", left.id, new Metadata(namespace: 'org.example', key: 'example_key_2', value: 'example_value_2'))

        DataModel right = dataModelApi.create(folderId,
                new DataModel(label: 'Test other data model', description: 'test other description', author: 'test author other'))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        objectDiff.diffs.size() == 6
        objectDiff.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL, PATH_IDENTIFIER].contains(it.name) }
        ArrayDiff<Collection> annotationsDiff = objectDiff.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.deleted.size() == 1
        annotationsDiff.created.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = objectDiff.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>

        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.deleted.size() == 2
        metadataDiff.created.isEmpty()
        metadataDiff.modified.isEmpty()
    }

    void 'test datamodel RHS - diff should have created, modified, boolean, diffs only '() {
        given:
        DataModel right = dataModelApi.show(left.id)

        DataModel left = dataModelApi.create(folderId,
            new DataModel(label: 'Test other data model',
                            description: 'test other description',
                            author: 'test author other'))

        and:
        Metadata metadataResponse = metadataApi.create("DataModel", left.id,
            new Metadata(namespace: 'org.example',
                         key: 'example_key',
                         value: 'different example_value'))

        metadataId = metadataResponse.id

        dataModelApi.finalise(left.id,
                              new FinaliseData(versionChangeType: VersionChangeType.MAJOR,
                                               versionTag: 'random version tag'))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        objectDiff.diffs.size() == 10
        objectDiff.diffs.each {
            [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL,
             PATH_IDENTIFIER, PATH_MODEL_IDENTIFIER, MODEL_VERSION_TAG, FINALISED, DATE_FINALISED].contains(it.name)
        }
        ArrayDiff<Collection> annotationsDiff = objectDiff.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.created.size() == 1
        annotationsDiff.deleted.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = objectDiff.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>
        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.created.isEmpty()
        metadataDiff.deleted.isEmpty()
        metadataDiff.modified.size() == 1
        metadataDiff.modified.diffs.size() == 1
        metadataDiff.modified.diffs.first().name.get(0) == 'value'
    }

    void 'test datamodel with annotations, modified, childAnnotations'() {
        given:
        DataModel right =
            dataModelApi.create(folderId, dataModelPayload())

        and:

        annotationApi.create("DataModel", right.id,
                new Annotation(
                    label: 'test-label',
                    description: 'different test-annotation description'))

        //childAnnotation
        Annotation child = annotationApi.create("DataModel", left.id, annotationId,
                annotationPayload('child label', 'child description'))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        objectDiff.diffs.size() == 2
        ArrayDiff<Collection> annotationsDiff = objectDiff.diffs.find { it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.created.isEmpty()
        annotationsDiff.deleted.isEmpty()

        annotationsDiff.modified.size() == 1

        ArrayDiff<Collection> childAnnotations = annotationsDiff.modified[0].diffs.find { it.name == DiffBuilder.CHILD_ANNOTATIONS }
        childAnnotations
        childAnnotations.created.isEmpty()
        childAnnotations.modified.isEmpty()
        childAnnotations.deleted.size() == 1
        childAnnotations.deleted[0].get(DiffBuilder.ID_KEY) == child.id.toString()
        childAnnotations.deleted[0].get(DiffBuilder.LABEL) == child.label
    }

    void 'test datamodel with same summaryMetadata (key= label) -should not appear in diff'() {
        given:
        DataModel right = dataModelApi.create(folderId, dataModelPayload())

        and:
        //2 same summaryMetadata payloads -for modified diff
        summaryMetadataApi.create("DataModel",right.id, summaryMetadataPayload())
        summaryMetadataApi.create("DataModel",left.id, summaryMetadataPayload())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        ArrayDiff<Collection> summaryMetadataDiff = objectDiff.diffs.find { it.name == DiffBuilder.SUMMARY_METADATA } as ArrayDiff<Collection>
        !summaryMetadataDiff

    }

    void 'test datamodel with modified summaryMetadata and summaryMetadataReport -should show nested diff'() {
        given:
        DataModel right = dataModelApi.create(folderId, dataModelPayload())

        and:
        summaryMetadataApi.create("DataModel",right.id, summaryMetadataPayload())

        SummaryMetadata leftSummaryMetadata = summaryMetadataApi.create("DataModel",left.id,
                    new SummaryMetadata(summaryMetadataType: SummaryMetadataType.MAP, label: 'summary metadata label'))

        SummaryMetadataReport report = summaryMetadataReportApi.create("DataModel", left.id, leftSummaryMetadata.id,
                summaryMetadataReport())

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        ArrayDiff<Collection> summaryMetadataDiff = objectDiff.diffs.find { it.name == DiffBuilder.SUMMARY_METADATA } as ArrayDiff<Collection>

        summaryMetadataDiff
        summaryMetadataDiff.created.isEmpty()
        summaryMetadataDiff.deleted.isEmpty()
        summaryMetadataDiff.modified.size() == 1
        summaryMetadataDiff.modified[0].diffs.find { [DiffBuilder.SUMMARY_METADATA_REPORT, DiffBuilder.SUMMARY_METADATA_TYPE].contains(it.name) }

        FieldDiff summaryMetadataTypeDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_TYPE } as FieldDiff
        summaryMetadataTypeDiffs.left == SummaryMetadataType.MAP.name()
        summaryMetadataTypeDiffs.right == SummaryMetadataType.STRING.name()
        ArrayDiff<Collection> reportDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_REPORT } as ArrayDiff<Collection>
        //reportDiffs.size() == 2
        reportDiffs.deleted.size() == 1
        reportDiffs.deleted[0].get(DiffBuilder.ID_KEY) == report.id.toString()
        reportDiffs.deleted[0].get(DiffBuilder.REPORT_DATE) == report.reportDate.toString()
    }
}
