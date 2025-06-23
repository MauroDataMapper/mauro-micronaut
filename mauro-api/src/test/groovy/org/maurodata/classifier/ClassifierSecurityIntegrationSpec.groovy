package org.maurodata.classifier


import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.Classifier
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse
import spock.lang.Ignore
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql"], phase = Sql.Phase.AFTER_EACH)
class ClassifierSecurityIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId


    @Shared
    UUID classificationSchemeId

    @Shared
    UUID classifierId1
    @Shared
    UUID classifierId2


    void setup() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        classificationSchemeId = classificationSchemeApi.create(folderId, classificationSchemePayload()).id
        classifierId1 = classifierApi.create(classificationSchemeId, classifierPayload()).id
        classifierId2 = classifierApi.create(classificationSchemeId, classifierPayload()).id
        logout()
    }

    void cleanup() {
        logout()
    }

    void 'list classifiers when admin user'() {
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

    @Ignore
    //toDo
    void 'list classifiers  - not logged in'() {
        when:
        ListResponse<Classifier> items = classifierApi.listAllClassifiers()
        then:
        items.items.isEmpty()
    }

}