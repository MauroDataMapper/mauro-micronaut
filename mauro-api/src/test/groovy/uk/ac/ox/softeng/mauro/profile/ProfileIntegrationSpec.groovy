package uk.ac.ox.softeng.mauro.profile

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.profile.MetadataNamespaceDTO
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.importexport.DataModelJsonImportExportIntegrationSpec
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.profile.test.DataModelBasedProfileTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import spock.lang.Shared

@ContainerizedTest
class ProfileIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    ObjectMapper objectMapper

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID dataModelId

    void setupSpec() {


        Folder folderResponse = (Folder) POST('/folders', [label: 'Test folder'], Folder)
        dataModelId = importDataModel(DataModelBasedProfileTest.testProfileModel, folderResponse)

    }


    void 'get namespaces'() {
        when:

        List<MetadataNamespaceDTO> namespaces = (List<MetadataNamespaceDTO>) GET('/metadata/namespaces', List<MetadataNamespaceDTO>)

        then:
        namespaces.size() == 3
        namespaces.find{ it.namespace == ProfileSpecificationFieldProfile.NAMESPACE}
        namespaces.find{ it.namespace == ProfileSpecificationProfile.NAMESPACE}

        namespaces.find{ it.namespace == "com.test.assets"}.keys == [
                "Asset Creation/Deleted date",
                "contactEmail",
                "createdDate",
                "priority",
                "retired",
                "size"
        ]
    }

    void "get used and unused profiles"() {

        when:
        List<Profile> unusedProfiles = (List<Profile>) GET("/dataModels/$dataModelId/profiles/unused", List<Profile>)

        then:
        unusedProfiles.size() == 1
        unusedProfiles.find { it.metadataNamespace == "com.test.assets" }


        when:
        List<Profile> usedProfiles = (List<Profile>) GET("/dataModels/$dataModelId/profiles/used", List<Profile>)

        then:
        usedProfiles.size() == 1
        usedProfiles.find { it.metadataNamespace == ProfileSpecificationProfile.NAMESPACE }

    }

}
