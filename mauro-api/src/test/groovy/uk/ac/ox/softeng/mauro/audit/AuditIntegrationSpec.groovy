package uk.ac.ox.softeng.mauro.audit

import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.facet.EditType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject

@SecuredContainerizedTest
class AuditIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application


    void "Test Audit annotation" () {
        when:
            loginAdmin()
            Folder folderResponse = (Folder) POST('/folders', [label: 'Test folder'], Folder)
            Map folderResponse2 = GET("/folders/${folderResponse.id}")
        then:
            folderResponse2.edits.size() == 1
            folderResponse2.edits.first().description == 'Created folder'
            folderResponse2.edits.first().title == EditType.CREATE.toString()

        when:
            ListResponse<Edit> editResponse = (ListResponse<Edit>) GET("/folders/${folderResponse.id}/edits", ListResponse, Edit)

        then:
            editResponse.items.size() == 1
            editResponse.items.first().description == 'Created folder'
            editResponse.items.first().title == EditType.CREATE
    }


}
