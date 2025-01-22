package uk.ac.ox.softeng.mauro.classifier

import uk.ac.ox.softeng.mauro.api.classifier.ClassificationSchemeApi

import io.micronaut.runtime.server.EmbeddedServer

import static uk.ac.ox.softeng.mauro.api.PathPopulation.populatePath
import uk.ac.ox.softeng.mauro.api.Paths

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

    @Shared
    UUID folderId

    @Inject
    ClassificationSchemeApi classificationSchemeApi

    void setup() {
        folderId = ((Folder) POST(Paths.FOLDER_LIST, folder(), Folder)).id
    }

    void 'list classification schemes - should return empty list'() {
        when:
        ListResponse<ClassificationScheme> listResponse = classificationSchemeApi.listAll()
        then:
        listResponse
        listResponse.items.isEmpty()
    }

    void 'add Classification scheme - should add'(){
        when:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST(
            populatePath(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE, ["folderId" : folderId]), classifiersPayload(), ClassificationScheme)

        then:
        classificationScheme

        when:
        ClassificationScheme retrieved = GET(populatePath(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE, ['id':classificationScheme.id]), ClassificationScheme)
        then:
        retrieved
        retrieved.authority
    }

    void 'get Classification scheme by ID - should return classification scheme'(){
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST(
            populatePath(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE, ["folderId" : folderId]), classifiersPayload(), ClassificationScheme)
        when:
        ClassificationScheme retrieved = GET(
            populatePath(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE, ['id':classificationScheme.id]), ClassificationScheme)

        then:
        retrieved
        retrieved.authority
    }

    void 'update classification scheme - should update model'() {
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)
        when:
        ClassificationScheme updated = (ClassificationScheme) PUT(
            populatePath(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE, ['id': classificationScheme.id]),
                [label: 'updated test label',
                 description: 'updated test description'],  ClassificationScheme)
        then:
        updated
        updated.label == 'updated test label'
        updated.description == 'updated test description'
        updated.authority
    }

    void 'delete classification scheme - should delete model and associations'() {
        given:
        ClassificationScheme classificationScheme = (ClassificationScheme) POST("$FOLDERS_PATH/$folderId$CLASSIFICATION_SCHEME_PATH", classifiersPayload(), ClassificationScheme)

        Classifier classifier = (Classifier) POST(
            populatePath(Paths.CLASSIFIERS_ROUTE, [
                'classificationSchemeId': classificationScheme.id]),
            classifiersPayload(), Classifier)

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier
        Classifier adminItemJoinClassifier1 = (Classifier) PUT (
            populatePath(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE,
                         ['administeredItemDomainType': 'classificationScheme',
                          'administeredItemId': classificationScheme.id,
                          'id': classifier.id ]), null, Classifier)
        Classifier adminItemJoinClassifier2 = (Classifier) PUT (
            populatePath(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE,
                         ['administeredItemDomainType': 'classifier',
                          'administeredItemId': classifier.id,
                          'id': classifier.id ]), null, Classifier)
        when:
        HttpStatus httpStatus = DELETE(populatePath(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE,['id': classificationScheme.id]), HttpStatus)

        then:
        httpStatus == HttpStatus.NO_CONTENT

    }

}
