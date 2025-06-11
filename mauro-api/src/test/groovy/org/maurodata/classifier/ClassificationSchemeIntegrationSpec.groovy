package org.maurodata.classifier

import org.maurodata.api.classifier.ClassificationSchemeApi
import org.maurodata.api.classifier.ClassifierApi
import org.maurodata.api.folder.FolderApi

import io.micronaut.http.HttpResponse
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

import org.maurodata.api.Paths

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_EACH)
class ClassificationSchemeIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    void setup() {
        folderId = folderApi.create(folder()).id
    }

    void 'list classification schemes - should return empty list'() {
        when:
        ListResponse<ClassificationScheme> listResponse = classificationSchemeApi.listAll()
        then:
        listResponse
        listResponse.items.isEmpty()
    }

    void 'add Classification scheme - should add'() {
        when:
        ClassificationScheme classificationScheme =
            classificationSchemeApi.create(folderId, classificationSchemePayload())

        then:
        classificationScheme

        when:
        ClassificationScheme retrieved =
            classificationSchemeApi.show(classificationScheme.id)
        then:
        retrieved
        retrieved.authority

        when:
        ListResponse<ClassificationScheme> classificationSchemes = classificationSchemeApi.listAll()

        then:
        classificationSchemes.items.size() == 1
    }

    void 'update classification scheme - should update model'() {
        given:
        ClassificationScheme classificationScheme =
            classificationSchemeApi.create(folderId, classificationSchemePayload())
        when:
        ClassificationScheme updated =
            classificationSchemeApi.update(classificationScheme.id,
                new ClassificationScheme(label: 'updated test label',
                 description: 'updated test description'))
        then:
        updated
        updated.label == 'updated test label'
        updated.description == 'updated test description'
        updated.authority
    }

    void 'delete classification scheme - should delete model and associations'() {
        given:
        ClassificationScheme classificationScheme =
            classificationSchemeApi.create(folderId, classificationSchemePayload())

        Classifier classifier =
            classifierApi.create(classificationScheme.id, classifierPayload())

        //Add AdministeredItemClassificationScheme to classifier -joinAdministeredItemToClassifier

        classifierApi.createAdministeredItemClassifier('classificationScheme', classificationScheme.id, classifier.id)
        classifierApi.createAdministeredItemClassifier('classifier', classifier.id, classifier.id)

        when:
        HttpResponse httpResponse =
            classificationSchemeApi.delete(classificationScheme.id, classificationScheme, true)

        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        // TODO: Keep testing this result...

    }

}
