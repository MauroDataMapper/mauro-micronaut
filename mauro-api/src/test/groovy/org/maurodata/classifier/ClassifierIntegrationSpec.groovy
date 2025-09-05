package org.maurodata.classifier

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_EACH)
class ClassifierIntegrationSpec extends CommonDataSpec {

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
        HttpResponse httpResponse = classifierApi.delete('dataModel', dataModel.id, classifier.id)

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

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

        DataModel dataModel = dataModelApi.create(folderId, new DataModel(label: 'Test data model'))

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier
        classifierApi.createAdministeredItemClassifier('dataModel', dataModel.id, classifier.id)

        when:
        HttpResponse httpResponse = classificationSchemeApi.delete(classificationSchemeId, new ClassificationScheme(), true)


        then:
        httpResponse.status == HttpStatus.NO_CONTENT


        //get datamodel
        when:
        DataModel retrieved = dataModelApi.show(dataModel.id)
        then:
        retrieved
        retrieved.classifiers.size() == 0

    }

    void 'list all classifiers from any classification scheme -should list all items in classifiers db'() {
        given:
        classifierApi.create(classificationSchemeId, classifierPayload('this label'))
        ClassificationScheme otherClassificationScheme = classificationSchemeApi.create(folderId, classificationSchemePayload())
        classifierApi.create(otherClassificationScheme.id, classifierPayload('other label'))
        when:
        ListResponse<Classifier> classifierList = classifierApi.listAllClassifiers()
        then:
        classifierList
        classifierList.items.size() == 2

    }
}
