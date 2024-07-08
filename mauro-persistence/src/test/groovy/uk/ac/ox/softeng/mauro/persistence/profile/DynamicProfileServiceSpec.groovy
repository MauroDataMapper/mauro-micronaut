package uk.ac.ox.softeng.mauro.persistence.profile

import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.Containerized
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.test.DataModelBasedProfileTest

@ContainerizedTest
class DynamicProfileServiceSpec extends Specification {

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DynamicProfileService dynamicProfileService

    def "Test getting all dynamic profiles"() {
        given:

        Folder myFirstFolder = folderRepository.save(new Folder(
                label: "My first Folder"
        ))

        when:
        DataModel testDataModel = DataModelBasedProfileTest.testProfileModel
        testDataModel.folder = myFirstFolder

        UUID dataModelId = dataModelContentRepository.saveWithContent(testDataModel).id


        DataModel saved = dataModelContentRepository.findWithContentById(dataModelId)
        System.err.println(saved.metadata.namespace)



        List<Profile> profiles = dynamicProfileService.getDynamicProfiles()

        then:
        profiles.size() == 1
        profiles[0].displayName == "Asset management profile"



    }

}
