package org.maurodata.classifier

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_ALL)
class ClassifierSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId


    @Shared
    UUID classificationSchemeId

    @Shared
    UUID classifierId1
    @Shared
    UUID classifierId2


    void setupSpec() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        classificationSchemeId = classificationSchemeApi.create(folderId, classificationSchemePayload(false, true)).id
        classifierId1 = classifierApi.create(classificationSchemeId, classifierPayload()).id
        classifierId2 = classifierApi.create(classificationSchemeId, classifierPayload()).id
        logout()
    }

    void cleanup() {
        logout()
    }


    void 'list classifiers when logged in as Admin'() {
        when:
        loginAdmin()

        then:
        classifierApi.listAllClassifiers().items.size() == 2

    }

    void 'list classifiers when logged in user'() {
        when:
        loginUser()

        then:
        classifierApi.listAllClassifiers().items.size() == 2
    }

    void 'list classifiers  - not logged in, parent models readableByEveryone flag = false'() {
        when:
        ListResponse<Classifier> items = classifierApi.listAllClassifiers()
        then:
        items.items.isEmpty()
    }

    void 'list classifiers  - not logged in, parent models readableByEveryone flag = true'() {
        given:
        loginAdmin()
        ClassificationScheme openClassificationScheme = classificationSchemeApi.create(folderId, classificationSchemePayload(true, true))
        classifierApi.create(openClassificationScheme.id, classifierPayload())
        logout()

        when:
        ListResponse<Classifier> items = classifierApi.listAllClassifiers()
        then:
        items.items.size() == 1
    }

}