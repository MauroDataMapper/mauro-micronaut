package uk.ac.ox.softeng.mauro.persistence.profile

import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.Containerized
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
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

    @Inject
    MetadataRepository metadataRepository

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


        List<Profile> profiles = dynamicProfileService.getDynamicProfiles()

        then:
        profiles.size() == 1
        profiles[0].displayName == "Asset management profile"
        profiles[0].keys == [
                "Asset Creation/Deleted date",
                "contactEmail",
                "createdDate",
                "priority",
                "retired",
                "size"
        ]

    }

    def "Test getting profile keys"() {
        given:

        Folder myFirstFolder = folderRepository.save(new Folder(
                label: "My first Folder"
        ))

        when:
        DataModel testDataModel = DataModelBasedProfileTest.testProfileModel
        testDataModel.folder = myFirstFolder

        dataModelContentRepository.saveWithContent(testDataModel).id

        List<Profile> profiles = dynamicProfileService.getDynamicProfiles()
        Profile dynamicProfile = profiles[0]

        then:


        metadataRepository.getNamespaceKeys() == [
                "uk.ac.ox.softeng.maurodatamapper.profile": ["metadataNamespace","profileApplicableForDomains"] as Set,
                "uk.ac.ox.softeng.maurodatamapper.profile.dataelement":["metadataPropertyName", "regularExpression"] as Set
        ]

    }

}