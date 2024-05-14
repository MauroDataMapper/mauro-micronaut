package uk.ac.ox.softeng.mauro.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.EmbeddedApplication
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

    void setupSpec() {
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

    void 'test datamodel diff -should diff models'() {
        given:
        Metadata metadataTwoResponse = (Metadata) POST("$DATAMODELS_PATH/$dataModelId$METADATA_PATH",
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
        diff.diffs.each { ['author', 'description', 'label', 'pathIdentifier'].contains(it.name) }
        ArrayDiff<Collection> annotationsDiff = diff.diffs.get(4) as ArrayDiff<Collection>
        annotationsDiff.name == DiffBuilder.ANNOTATION
        annotationsDiff.created.size() == 1
        annotationsDiff.deleted.isEmpty()
        annotationsDiff.modified.isEmpty()

        ArrayDiff<Collection> metadataDiff = diff.diffs.get(5) as ArrayDiff<Collection>
        metadataDiff.name == DiffBuilder.METADATA
        metadataDiff.created.size() == 2
        metadataDiff.deleted.isEmpty()
        metadataDiff.modified.isEmpty()
    }

}
