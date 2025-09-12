package org.maurodata.facet

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import jakarta.mail.Folder
import org.maurodata.domain.facet.Annotation
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@Singleton
@SecuredContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-annotation.sql", phase = Sql.Phase.AFTER_ALL)
class AnnotationSecuredIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId
    @Shared
    UUID annotationId

    void setupSpec() {
        loginAdmin()
        folderId = folderApi.create(folder()).id
        annotationId = annotationApi.create("folder", folderId, annotationPayload()).id
        logout()
    }


    void 'get annotation by Id -should return annotation with createdByUser information'() {
        given:
        loginAdmin()

        when:
        Annotation retrieved = annotationApi.show("folder", folderId, annotationId)

        then:
        retrieved
        retrieved.createdByUser
        retrieved.createdByUser.id
    }

    void 'list annotations -should show createdByUser information in response '() {
        given:
        loginAdmin()
        and:
        annotationApi.create(Folder.simpleName, folderId, annotationId, annotationPayload('child label', 'child descripton'))

        when:
        ListResponse<Annotation> listResponse = annotationApi.list(Folder.simpleName, folderId)

        then:
        listResponse
        listResponse.items.size() == 1
        listResponse.items[0].createdByUser?.id
        listResponse.items[0].childAnnotations.size() == 1
        listResponse.items[0].childAnnotations[0].createdByUser?.id
    }
}
