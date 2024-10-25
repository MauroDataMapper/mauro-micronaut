package uk.ac.ox.softeng.mauro.classifier

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_EACH)
class ClassifierIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID classificationSchemeId

    void setup() {
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        classificationSchemeId = ((ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)).id
    }


    void 'add Classifier -should add'() {
        when:
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)
        then:
        classifier

        when:
        Classifier retrieved = (Classifier) GET("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id", Classifier)
        then:
        retrieved

        when:
        ListResponse<Classifier> classifiers = (ListResponse<Classifier>) GET("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", ListResponse<Classifier>)
        then:
        classifiers
        classifiers.items.size() == 1
        classifiers.items[0].id == retrieved.id.toString()
    }


    void 'update classifier - should update model'() {
        given:
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)

        when:
        Classifier updated = (Classifier) PUT("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id",
                [label: 'updated test label',
                 description: 'updated test description'],  Classifier)
        then:
        updated
        updated.label.contains('updated')
        updated.description.contains('updated')
    }


    void 'add child Classifier -should add '() {
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)

        when:
        Classifier child = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id$CLASSIFIER_PATH",
            [   label: 'Test child',
                description : 'Test child random description ',
                readableByEveryone: true,
                readableByAuthenticatedUsers: true ], Classifier)
        then:
        child

        when:
        Classifier retrieved =  (Classifier) GET("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$child.id", Classifier)
        then:
        retrieved
        retrieved.label == 'Test child'
        retrieved.description == 'Test child random description '

    }

    void 'update child Classifier -should update '() {
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)

        when:
        Classifier child = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id$CLASSIFIER_PATH",
                classifiersPayload(), Classifier)

        then:
        child
        !child.label.contains('Updated')
        !child.description.contains('Updated')

        when:
        Classifier updated = (Classifier) PUT("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id$CLASSIFIER_PATH/$child.id",
                [   label: 'Updated Test child',
                    description : 'Updated Test child random description ',
                    readableByEveryone: true,
                    readableByAuthenticatedUsers: true ], Classifier)
        then:
        updated
        updated.label.contains('Updated')
        updated.description.contains('Updated')
    }


    void 'Add administered item to classifier  - should add item'() {
        given:
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)

        when:
        Folder getFolder = (Folder) GET ("$FOLDERS_PATH/$folderId", Folder)
        then:
        getFolder
        getFolder.classifiers.isEmpty()

        when:
        //add folder as admin item to classifier
        Map<String, Object> response = (Map) PUT("/folder/$folderId$CLASSIFIER_PATH/$classifier.id", Classifier)

        then:
        response
        response.get('id') == classifier.id.toString()

        when:
        Folder getFolderResponse = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)
        then:
        getFolderResponse
        getFolderResponse.classifiers.size() == 1
        getFolderResponse.classifiers.first().id == classifier.id
    }


    void 'Delete administered item to classifier  - should delete join only '() {
        given:
        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH", classifiersPayload(), Classifier)
        DataModel dataModel = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)

        and:
        PUT("/dataModel/$dataModel.id$CLASSIFIER_PATH/$classifier.id", Classifier)

        Classifier joinAdministeredItemToClassifierRetrieved = (Classifier) GET("/dataModel/$dataModel.id$CLASSIFIER_PATH/$classifier.id", Classifier)
        joinAdministeredItemToClassifierRetrieved


        when:
        HttpStatus httpStatus = (HttpStatus) DELETE("/dataModel/$dataModel.id$CLASSIFIER_PATH/$classifier.id",  HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        when:
        Classifier existing = (Classifier) GET("$CLASSIFICATION_SCHEME_PATH/$classificationSchemeId$CLASSIFIER_PATH/$classifier.id", Classifier)
        then:
        existing
        existing.id == classifier.id

        when:
        DataModel retrievedDataModel = (DataModel) GET("$DATAMODELS_PATH/$dataModel.id", DataModel)
        then:
        retrievedDataModel
        retrievedDataModel.id == dataModel.id
    }

    void 'delete classifier - should delete model and associations'() {
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)

        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id$CLASSIFIER_PATH", classifiersPayload(), Classifier)
        //add facet to classifier
        SummaryMetadata summaryMetadata  = (SummaryMetadata) POST ("/classifier/$classifier.id/summaryMetadata", summaryMetadataPayload(), SummaryMetadata)

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier
        PUT("/classificationScheme/$classificationScheme.id$CLASSIFIER_PATH/$classifier.id",  Classifier)
        PUT("/classifier/$classifier.id$CLASSIFIER_PATH/$classifier.id", Classifier)

        DataModel dataModel = (DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", dataModelPayload(), DataModel)
        PUT("/dataModel/$dataModel.id$CLASSIFIER_PATH/$classifier.id", Classifier)

        when:
        HttpStatus httpStatus = DELETE("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT


        //get datamodel
        when:
        DataModel retrieved = (DataModel) GET("$DATAMODELS_PATH/$dataModel.id", DataModel)
        then:
        retrieved

    }

}
