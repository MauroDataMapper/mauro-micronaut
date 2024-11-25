package uk.ac.ox.softeng.mauro.datamodel.diff

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelFacetDiffsIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId
    @Shared
    DataModel left

    @Shared
    UUID metadataId

    @Shared
    UUID annotationId

    void setup() {
        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        left = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        Metadata metadataResponse = (Metadata) POST("$DATAMODELS_PATH/$left.id$METADATA_PATH", metadataPayload(), Metadata)
        metadataId = metadataResponse.id

        Annotation annotationResponse = (Annotation) POST("$DATAMODELS_PATH/$left.id/$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        annotationId = annotationResponse.id
    }

    void 'test datamodel diff - lhs has facets - diff should have deleted items only'() {
        given:
        (Metadata) POST("$DATAMODELS_PATH/$left.id$METADATA_PATH",
                [namespace: 'org.example', key: 'example_key_2', value: 'example_value_2'], Metadata)

        DataModel right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test other data model', description: 'test other description', author: 'test author other'], DataModel)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id$DIFF/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.label == left.label
        diffMap.diffs.size() == 6
        diffMap.diffs.each { [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL, PATH_IDENTIFIER].contains(it.name) }
        ArrayDiff<Collection> annotationsDiff = diffMap.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.deleted.size() == 1
        annotationsDiff.created.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = diffMap.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>

        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.deleted.size() == 2
        metadataDiff.created.isEmpty()
        metadataDiff.modified.isEmpty()
    }

    void 'test datamodel RHS - diff should have created, modified, boolean, diffs only '() {
        given:
        DataModel right = (DataModel) GET("$DATAMODELS_PATH/$left.id", DataModel)

        DataModel left = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test other data model', description: 'test other description', author: 'test author other'], DataModel)

        and:
        Metadata metadataResponse = (Metadata) POST("$DATAMODELS_PATH/$left.id$METADATA_PATH", [namespace: 'org.example', key: 'example_key', value: 'different example_value'],
                Metadata)
        metadataId = metadataResponse.id

        (DataModel) PUT("$DATAMODELS_PATH/$left.id/finalise", [versionChangeType: 'major', versionTag: 'random version tag'],
                DataModel)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id$DIFF/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.label == left.label
        diffMap.diffs.size() == 10
        diffMap.diffs.each {
            [AUTHOR, DiffBuilder.DESCRIPTION, DiffBuilder.LABEL,
             PATH_IDENTIFIER, PATH_MODEL_IDENTIFIER, MODEL_VERSION_TAG, FINALISED, DATE_FINALISED].contains(it.name)
        }
        ArrayDiff<Collection> annotationsDiff = diffMap.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.created.size() == 1
        annotationsDiff.deleted.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = diffMap.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>
        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.created.isEmpty()
        metadataDiff.deleted.isEmpty()
        metadataDiff.modified.size() == 1
        metadataDiff.modified.diffs.size() == 1
        metadataDiff.modified.diffs.first().name.get(0) == 'value'
    }

    void 'test datamodel with annotations, modified, childAnnotations'() {
        given:
        DataModel right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        and:

        (Annotation) POST("$DATAMODELS_PATH/$right.id$ANNOTATION_PATH",
                [label: 'test-label', description: 'different test-annotation description'], Annotation)

        //childAnnotation
        Annotation child = (Annotation) POST("$DATAMODELS_PATH/$left.id$ANNOTATION_PATH/$annotationId$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id$DIFF/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.label == left.label
        diffMap.diffs.size() == 2
        ArrayDiff<Collection> annotationsDiff = diffMap.diffs.find { it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
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
        DataModel right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        and:
        //2 same summaryMetadata payloads -for modified diff
        POST("$DATAMODELS_PATH/$right.id$SUMMARY_METADATA_PATH", summaryMetadataPayload(), SummaryMetadata)

        POST("$DATAMODELS_PATH/$left.id$SUMMARY_METADATA_PATH", summaryMetadataPayload(), SummaryMetadata)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id$DIFF/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.label == left.label
        ArrayDiff<Collection> summaryMetadataDiff = diffMap.diffs.find { it.name == DiffBuilder.SUMMARY_METADATA } as ArrayDiff<Collection>
        !summaryMetadataDiff

    }

    void 'test datamodel with modified summaryMetadata and summaryMetadataReport -should show nested diff'() {
        given:
        DataModel right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        and:
        POST("$DATAMODELS_PATH/$right.id$SUMMARY_METADATA_PATH", summaryMetadataPayload(), SummaryMetadata)

        SummaryMetadata leftSummaryMetadata = POST("$DATAMODELS_PATH/$left.id$SUMMARY_METADATA_PATH",
                [summaryMetadataType: SummaryMetadataType.MAP, label: 'summary metadata label'], SummaryMetadata)

        SummaryMetadataReport report = (SummaryMetadataReport) POST("$DATAMODELS_PATH/$left.id$SUMMARY_METADATA_PATH/$leftSummaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)

        when:
        Map<String, Object> diffMap = GET("$DATAMODELS_PATH/$left.id$DIFF/$right.id", Map<String, Object>)

        then:
        diffMap
        diffMap.get(DiffBuilder.LABEL) == left.label
        ArrayDiff<Collection> summaryMetadataDiff = diffMap.diffs.find { it.name == DiffBuilder.SUMMARY_METADATA } as ArrayDiff<Collection>

        summaryMetadataDiff
        summaryMetadataDiff.created.isEmpty()
        summaryMetadataDiff.deleted.isEmpty()
        summaryMetadataDiff.modified.size() == 1
        summaryMetadataDiff.modified[0].diffs.find { [DiffBuilder.SUMMARY_METADATA_REPORT, DiffBuilder.SUMMARY_METADATA_TYPE].contains(it.name) }

        Map<String, String> summaryMetadataTypeDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_TYPE } as Map<String, String>
        summaryMetadataTypeDiffs.get('left') == SummaryMetadataType.MAP.name()
        summaryMetadataTypeDiffs.get('right') == SummaryMetadataType.STRING.name()
        Map<String, Object> reportDiffs = summaryMetadataDiff.modified[0].diffs.find { it.name == DiffBuilder.SUMMARY_METADATA_REPORT } as Map<String, Object>
        reportDiffs.size() == 2
        reportDiffs.get('deleted').size() == 1
        reportDiffs.get('deleted')[0].get(DiffBuilder.ID_KEY) == report.id.toString()
        reportDiffs.get('deleted')[0].get(DiffBuilder.REPORT_DATE) == report.reportDate.toString()
    }
}
