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
            Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
            Folder folderResponse2 = folderApi.show(folderResponse.id)
        then:
            folderResponse2.edits.size() == 1
            folderResponse2.edits.first().description == 'Created folder'
            folderResponse2.edits.first().title == EditType.CREATE

        when:
            ListResponse<Edit> editResponse = editApi.list("folder", folderResponse.id)

        then:
            editResponse.items.size() == 1
            editResponse.items.sort { it.dateCreated }.first().description == 'Created folder'
            editResponse.items.sort { it.dateCreated }.first().title == EditType.CREATE

        when:
            folderApi.update(folderResponse.id, new Folder(label: "Test folder (updated)"))
            folderResponse2 = folderResponse2 = folderApi.show(folderResponse.id)

        then:
            folderResponse2.edits.size() == 2
            folderResponse2.edits.sort { it.dateCreated }.last().description == 'Updated folder'
            folderResponse2.edits.sort { it.dateCreated }.last().title == EditType.UPDATE

        when:
            editResponse = editApi.list("folder", folderResponse.id)

        then:
            editResponse.items.size() == 2
            editResponse.items.sort { it.dateCreated }.last().description == 'Updated folder'
            editResponse.items.sort { it.dateCreated }.last().title == EditType.UPDATE
            logout()
    }


}
