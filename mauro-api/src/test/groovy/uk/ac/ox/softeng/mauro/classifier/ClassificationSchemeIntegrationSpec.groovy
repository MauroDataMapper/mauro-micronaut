package uk.ac.ox.softeng.mauro.classifier

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_EACH)
class ClassificationSchemeIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId


    void setupSpec(){
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
    }


    void 'list classification schemes -should return empty list'() {
        when:
        ListResponse<ClassificationScheme> listResponse = (ListResponse<ClassificationScheme>) GET("$CLASSIFICATION_SCHEME_PATH", ListResponse<ClassificationScheme>)
        then:
        listResponse
        listResponse.items.isEmpty()
    }

    void 'add Classification scheme -should add'() {
        when:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)
        then:
        classificationScheme

        when:
        ClassificationScheme retrieved = GET("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id", ClassificationScheme)
        then:
        retrieved
        retrieved.authority
    }

    void 'get Classification scheme by ID -should return classification scheme'(){
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)
        when:
        ClassificationScheme retrieved = GET("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id", ClassificationScheme)

        then:
        retrieved
        retrieved.authority
    }

    void 'update classification scheme -should update model'() {
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)
        when:
        ClassificationScheme updated = (ClassificationScheme) PUT("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id",
                [label: 'updated test label',
                 description: 'updated test description'],  ClassificationScheme)
        then:
        updated
        updated.label == 'updated test label'
        updated.description == 'updated test description'
        updated.authority
    }

    void 'delete classification scheme -should delete model and associations'() {
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)

        Classifier classifier = (Classifier) POST("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id$CLASSIFIER_PATH", classifiersPayload(), Classifier)

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier
        Classifier adminItemJoinClassifier1 = (Classifier) PUT ("/classificationScheme/$classificationScheme.id$CLASSIFIER_PATH/$classifier.id",null, Classifier)
        Classifier adminItemJoinClassifier2 = (Classifier) PUT ("/classifier/$classifier.id$CLASSIFIER_PATH/$classifier.id",null, Classifier)

        when:
        HttpStatus httpStatus = DELETE("$CLASSIFICATION_SCHEME_PATH/$classificationScheme.id", HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

    }

}
