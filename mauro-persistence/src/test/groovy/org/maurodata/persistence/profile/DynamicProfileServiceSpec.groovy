package org.maurodata.persistence.profile

import jakarta.inject.Inject
import spock.lang.Specification
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.Containerized
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.profile.Profile
import org.maurodata.profile.test.DataModelBasedProfileTest

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
                "org.maurodata.profile": ["metadataNamespace","profileApplicableForDomains"] as Set,
                "org.maurodata.profile.dataelement":["metadataPropertyName", "regularExpression"] as Set
        ]

    }

}