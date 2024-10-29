package uk.ac.ox.softeng.mauro.datamodel

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
class DataModelWithClassifiersIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId
    @Shared
    UUID classificationSchemeId
    @Shared
    UUID classifierId1
    @Shared
    UUID classifierId2

    void setupSpec(){
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        classificationSchemeId = ((ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)).id
        classifierId1 = ((Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)).id
        classifierId2 = ((Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)).id
    }


    void 'create datamodel with classifier -should create'() {
        when:
        DataModel created  = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH",
                [label: 'test label',
                 description: 'test description',
                 author: 'an author',
                 classifiers: [
                         [id: classifierId1]]],  DataModel)
        then:
        created
        created.classifiers.size() == 1
        created.classifiers[0].id == classifierId1

        when:
        Classifier retrieved1 = (Classifier) GET("$DATAMODELS_PATH/$created.id$CLASSIFIER_PATH/$classifierId1", Classifier)
        then:
        retrieved1
        retrieved1.id == classifierId1

        when:
        GET("$DATAMODELS_PATH/$created.id$CLASSIFIER_PATH/$classifierId2", Classifier)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'create datamodel with non existing classifier -should throw exception indicating classifier not found'() {
        when:
        POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH",
                [label: 'test label',
                 description: 'test description',
                 author: 'an author',
                         classifiers: [
                             [id:  UUID.randomUUID()]]],  DataModel)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'update datamodel with non existing classifier -should throw exception indicating classifier not found'() {
        when:
        DataModel created  = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)
        then:
        created

        when:
        PUT("$FOLDERS_PATH/$folderId$DATAMODELS_PATH/$created.id",
                [  classifiers: [ [id:  UUID.randomUUID()]]],  DataModel)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }



    void 'update datamodel with classifier -should update'() {
        when:
        DataModel created  = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)
        then:
        created

        when:
        DataModel updated  = (DataModel) PUT("$DATAMODELS_PATH/$created.id", [  classifiers: [ [id:  classifierId2]]],  DataModel)
        then:
        updated
        updated.classifiers.size() == 1
        updated.classifiers[0].id == classifierId2

        when:
        Classifier retrieved = (Classifier) GET("$DATAMODELS_PATH/$created.id$CLASSIFIER_PATH/$classifierId2", Classifier)
        then:

        retrieved
        retrieved.id == classifierId2

        when:
        GET("$DATAMODELS_PATH/$created.id$CLASSIFIER_PATH/$classifierId1", Classifier)
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }


    void 'delete datamodel with classifier and facets - should delete all items'() {
        given:
        DataModel dataModel  = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH",
                [label: 'test label',
                 description: 'test description',
                 author: 'an author',
                 classifiers: [
                         [id: classifierId1]]],  DataModel)

        Metadata metadata = (Metadata) POST("$DATAMODELS_PATH/$dataModel.id$METADATA_PATH", metadataPayload(), Metadata)

        when:
        HttpStatus status = DELETE("$DATAMODELS_PATH/$dataModel.id", HttpStatus)
        then:
        status == HttpStatus.NO_CONTENT

        when:
        GET("$DATAMODELS_PATH/$dataModel.id/$metadata.id", Metadata)

        then:
        HttpClientResponseException exc = thrown()
        exc.status == HttpStatus.NOT_FOUND

        when:
        GET("$DATAMODELS_PATH/$dataModel.id/$classifierId1", Classifier)

        then:
        exc = thrown()
        exc.status == HttpStatus.NOT_FOUND
    }
}
