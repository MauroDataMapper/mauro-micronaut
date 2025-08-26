package org.maurodata.datamodel.diff


import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.diff.ArrayDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec

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
        objectDiff.diffs.size() == 5
        objectDiff.diffs.every { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL, DiffBuilder.ANNOTATION, DiffBuilder.METADATA].contains(it.name) }
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
        objectDiff.diffs.size() == 9
        objectDiff.diffs.every {
            [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL,
             PATH_MODEL_IDENTIFIER, MODEL_VERSION_TAG, FINALISED, DATE_FINALISED, DiffBuilder.ANNOTATION, DiffBuilder.METADATA].contains(it.name)
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
        ArrayDiff<Collection> summaryMetadataDiff = objectDiff.diffs.find {it.name == DiffBuilder.SUMMARY_METADATA} as ArrayDiff<Collection>
        !summaryMetadataDiff

    }

    void 'test datamodel with modified summaryMetadata and summaryMetadataReport -should show nested diff'() {
        given:
        DataModel right = dataModelApi.create(folderId, dataModelPayload())

        and:
        summaryMetadataApi.create("DataModel",right.id, summaryMetadataPayload())

        SummaryMetadata leftSummaryMetadata = summaryMetadataApi.create("DataModel",left.id,
                    new SummaryMetadata(summaryMetadataType: SummaryMetadataType.MAP, label: 'summary metadata label'))

        SummaryMetadataReport leftReport = summaryMetadataReportApi.create("DataModel", left.id, leftSummaryMetadata.id,
                summaryMetadataReport('left report label', REPORT_DATE))

        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)

        then:
        objectDiff
        objectDiff.label == left.label
        ArrayDiff<Collection> summaryMetadataDiff = objectDiff.diffs.find {it.name == DiffBuilder.SUMMARY_METADATA} as ArrayDiff<Collection>

        summaryMetadataDiff
        summaryMetadataDiff.created.isEmpty()
        summaryMetadataDiff.deleted.isEmpty()
        summaryMetadataDiff.modified.size() == 1
        summaryMetadataDiff.modified[0].diffs.find { [DiffBuilder.SUMMARY_METADATA_REPORT, DiffBuilder.SUMMARY_METADATA_TYPE].contains(it.name) }

        FieldDiff summaryMetadataTypeDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_TYPE } as FieldDiff
        summaryMetadataTypeDiffs.left == SummaryMetadataType.MAP.name()
        summaryMetadataTypeDiffs.right == SummaryMetadataType.STRING.name()

        ArrayDiff<Collection> reportDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_REPORT }
        reportDiffs.deleted.size() == 1
        reportDiffs.deleted[0].get(DiffBuilder.ID_KEY) == leftReport.id.toString()
        reportDiffs.deleted[0].get(DiffBuilder.REPORT_DATE) == leftReport.reportDate.toString()
        reportDiffs.deleted[0].get(DiffBuilder.REPORT_VALUE) == leftReport.reportValue
    }

    void 'test datamodel - self does not have summaryMetadata -should show modified reportValue '() {
        given:
        DataModel right = dataModelApi.create(folderId, dataModelPayload())

        and:
        SummaryMetadata rightSummaryMetadata = summaryMetadataApi.create("DataModel", right.id,
                                                                        new SummaryMetadata(summaryMetadataType: SummaryMetadataType.MAP, label: 'summary metadata label right'))

        SummaryMetadataReport rightReport = summaryMetadataReportApi.create("DataModel", right.id, rightSummaryMetadata.id,
                                                                       summaryMetadataReport('right report label', REPORT_DATE))
        SummaryMetadata leftSummaryMetadata = summaryMetadataApi.create("DataModel", left.id,
                                                                         new SummaryMetadata(summaryMetadataType: SummaryMetadataType.MAP, label: 'summary metadata label left'))
        SummaryMetadataReport leftReport = summaryMetadataReportApi.create("DataModel", left.id, leftSummaryMetadata.id,
                                                                            summaryMetadataReport('left report label', REPORT_DATE))
        when:
        ObjectDiff objectDiff = dataModelApi.diffModels(left.id, right.id)
        then:
        objectDiff
        objectDiff.label == left.label
        ArrayDiff<Collection> summaryMetadataDiff = objectDiff.diffs.find {it.name == DiffBuilder.SUMMARY_METADATA} as ArrayDiff<Collection>

        summaryMetadataDiff
        summaryMetadataDiff.modified.size() == 1
        summaryMetadataDiff.created.size() == 0
        summaryMetadataDiff.deleted.size() == 0

        summaryMetadataDiff.modified[0].diffs[0].modified[0].diffs[0].left==leftReport.reportValue
        summaryMetadataDiff.modified[0].diffs[0].modified[0].diffs[0].right==rightReport.reportValue
    }
}
