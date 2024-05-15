package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-datamodel.sql", phase = Sql.Phase.AFTER_EACH)
class DataModelDiffsIntegrationSpec extends CommonDataSpec {

    @Inject
    ObjectMapper objectMapper

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId
    @Shared
    UUID dataModelId

    @Shared
    UUID metadataId

    @Shared
    UUID annotationId

    void setup() {
        Folder response = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = response.id

        DataModel dataModelResponse = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test data model', description: 'test description', author: 'test author'], DataModel)
        dataModelId = dataModelResponse.id

        Metadata metadataResponse = (Metadata) POST("$DATAMODELS_PATH/$dataModelId$METADATA_PATH", metadataPayload(), Metadata)
        metadataId = metadataResponse.id


        Annotation annotationResponse = (Annotation) POST("$DATAMODELS_PATH/$dataModelId/$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        annotationId = annotationResponse.id
    }

    void 'test datamodel diff - lhs has facets - diff should have deleted items only'() {
        given:
        (Metadata) POST("$DATAMODELS_PATH/$dataModelId$METADATA_PATH",
                [namespace: 'org.example', key: 'example_key_2', value: 'example_value_2'], Metadata)

       
        DataModel left = (DataModel) GET("$DATAMODELS_PATH/$dataModelId", DataModel)

        DataModel right = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test other data model', description: 'test other description', author: 'test author other'], DataModel)

        when:
        ObjectDiff diff = left.diff(right)

        then:
        diff
        diff.label == left.label
        diff.diffs.size() == 6
        diff.getNumberOfDiffs() == 7
        diff.diffs.each { [AUTHOR, DESCRIPTION, LABEL, PATH_IDENTIFIER].contains(it.name) }
        ArrayDiff<Collection> annotationsDiff = diff.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.deleted.size() == 1
        annotationsDiff.created.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = diff.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>

        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.deleted.size() == 2
        metadataDiff.created.isEmpty()
        metadataDiff.modified.isEmpty()
    }

    void 'test datamodel RHS - diff should have created, modified, boolean, diffs only '() {
        given:
        DataModel right = (DataModel) GET("$DATAMODELS_PATH/$dataModelId", DataModel)

        DataModel left = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'Test other data model', description: 'test other description', author: 'test author other'], DataModel)

        and:
        Metadata metadataResponse = (Metadata) POST("$DATAMODELS_PATH/$left.id$METADATA_PATH", [namespace: 'org.example', key: 'example_key', value: 'different example_value'],
                Metadata)
        metadataId = metadataResponse.id

        DataModel finalised = (DataModel) PUT("$DATAMODELS_PATH/$left.id/finalise", [versionChangeType: 'major', versionTag: 'random version tag'],
                DataModel)

        when:
        ObjectDiff diff = finalised.diff(right)

        then:
        String output = objectMapper.writeValueAsString(diff)
        diff
        diff.label == left.label
        diff.diffs.size() == 10
        diff.getNumberOfDiffs() == 10
        diff.diffs.each { [AUTHOR, DESCRIPTION, LABEL, PATH_IDENTIFIER].contains(it.name) }
        diff.diffs.each { [AUTHOR, DESCRIPTION, LABEL, PATH_IDENTIFIER, MODEL_VERSION_TAG, FINALISED, DATE_FINALISED].contains(it.name) }
        ArrayDiff<Collection> annotationsDiff = diff.diffs.find { it -> it.name == DiffBuilder.ANNOTATION } as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.created.size() == 1
        annotationsDiff.deleted.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = diff.diffs.find { it -> it.name == DiffBuilder.METADATA } as ArrayDiff<Collection>
        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.created.isEmpty()
        metadataDiff.deleted.isEmpty()
        metadataDiff.modified.size() == 1
        metadataDiff.modified.numberOfDiffs.get(0) == 1
        metadataDiff.modified.diffs.size() == 1
        metadataDiff.modified.diffs.first().name.get(0) == 'value'
    }
}
