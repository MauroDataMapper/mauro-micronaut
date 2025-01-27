package uk.ac.ox.softeng.mauro.classifier

import uk.ac.ox.softeng.mauro.api.classifier.ClassificationSchemeApi
import uk.ac.ox.softeng.mauro.api.classifier.ClassifierApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.controller.Application
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import io.micronaut.test.extensions.spock.annotation.MicronautTest
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

    @Inject ClassificationSchemeApi classificationSchemeApi
    @Inject ClassifierApi classifierApi
    @Inject FolderApi folderApi
    @Inject DataModelApi dataModelApi
    @Inject SummaryMetadataApi summaryMetadataApi

    @Shared
    UUID folderId

    @Shared
    UUID classificationSchemeId

    void setup() {
        folderId = folderApi.create(folder()).id
        classificationSchemeId = classificationSchemeApi.create(folderId, classificationSchemePayload()).id
    }


    void 'add Classifier -should add'() {
        when:
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())
        then:
        classifier

        when:
        Classifier retrieved = classifierApi.show(classificationSchemeId, classifier.id)
        then:
        retrieved

        when:
        ListResponse<Classifier> classifiers = classifierApi.list(classificationSchemeId)
        then:
        classifiers
        classifiers.items.size() == 1
        classifiers.items.first().id == retrieved.id
    }


    void 'update classifier - should update model'() {
        given:
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())

        when:
        Classifier updated = classifierApi.update(
            classificationSchemeId,
            classifier.id,
            new Classifier(label: 'updated test label', description: 'updated test description'))

        then:
        updated
        updated.label.contains('updated')
        updated.description.contains('updated')
    }


    void 'add child Classifier -should add '() {
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())

        when:
        Classifier child = classifierApi.create(
            classificationSchemeId,
            classifier.id,
            new Classifier(
                label: 'Test child',
                description : 'Test child random description '))
        then:
        child

        when:
        Classifier retrieved =  classifierApi.show(classificationSchemeId,child.id)
        then:
        retrieved
        retrieved.label == 'Test child'
        retrieved.description == 'Test child random description '

    }

    void 'update child Classifier -should update '() {
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())

        when:
        Classifier child = classifierApi.create(
            classificationSchemeId,
            classifier.id,
            new Classifier(
                label: 'Test child',
                description : 'Test child random description '))

        then:
        child
        !child.label.contains('Updated')
        !child.description.contains('Updated')

        when:
        Classifier updated = classifierApi.update(
            classificationSchemeId,
            classifier.id,
            child.id,
            new Classifier(
                label: 'Updated Test child',
                description : 'Updated Test child random description '))
        then:
        updated
        updated.label.contains('Updated')
        updated.description.contains('Updated')
    }


    void 'Add administered item to classifier  - should add item'() {
        given:
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())

        when:
        Folder getFolder = folderApi.show(folderId)
        then:
        getFolder
        getFolder.classifiers.isEmpty()

        when:
        //add folder as admin item to classifier
        Classifier classifierResponse = classifierApi.createAdministeredItemClassifier("folder", folderId, classifier.id)

        then:
        classifierResponse
        classifierResponse.id == classifier.id

        when:
        Folder getFolderResponse = folderApi.show(folderId)
        then:
        getFolderResponse
        getFolderResponse.classifiers.size() == 1
        getFolderResponse.classifiers.first().id == classifier.id
    }


    void 'Delete administered item to classifier  - should delete join only '() {
        given:
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())

        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Test data model'))

        and:
        classifierApi.createAdministeredItemClassifier('dataModel', dataModel.id, classifier.id)

        Classifier joinAdministeredItemToClassifierRetrieved = classifierApi.getAdministeredItemClassifier('dataModel', dataModel.id, classifier.id)
        joinAdministeredItemToClassifierRetrieved

        and:
        DataModel existingDataModel = dataModelApi.show(dataModel.id)
        existingDataModel.classifiers.size() == 1

        when:
        HttpStatus httpStatus = classifierApi.delete('dataModel',dataModel.id,classifier.id)

        then:
        httpStatus == HttpStatus.NO_CONTENT

        when:
        Classifier existing = classifierApi.show(classificationSchemeId, classifier.id)
        then:
        existing
        existing.id == classifier.id

        when:
        DataModel retrievedDataModel = dataModelApi.show(dataModel.id)
        then:
        retrievedDataModel
        retrievedDataModel.id == dataModel.id
        retrievedDataModel.classifiers.isEmpty()
    }

    void 'delete classifier - should delete model and associations'() {
        given:
        Classifier classifier = classifierApi.create(classificationSchemeId, classifierPayload())
        //add facet to classifier
        SummaryMetadata summaryMetadata =
            summaryMetadataApi.create(
    'classifier',
                classifier.id,
                new SummaryMetadata(summaryMetadataType: SummaryMetadataType.STRING, label: 'summary metadata label'))

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier

        classifierApi.createAdministeredItemClassifier('classificationScheme', classificationSchemeId, classifier.id)
        classifierApi.createAdministeredItemClassifier('classifier', classifier.id, classifier.id)

        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Test data model'))
        classifierApi.createAdministeredItemClassifier('dataModel', dataModel.id, classifier.id)

        when:
        HttpStatus httpStatus = classificationSchemeApi.delete(classificationSchemeId, new ClassificationScheme())


        then:
        httpStatus == HttpStatus.NO_CONTENT


        //get datamodel
        when:
        DataModel retrieved = dataModelApi.show(dataModel.id)
        then:
        retrieved
        retrieved.classifiers.size() == 0

    }

}
