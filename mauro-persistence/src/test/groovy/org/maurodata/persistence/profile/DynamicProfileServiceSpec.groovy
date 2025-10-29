package org.maurodata.persistence.profile

import jakarta.inject.Inject
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.datamodel.DataModelRepository
import spock.lang.Shared
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
    ContentsService contentsService

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    DynamicProfileService dynamicProfileService

    @Inject
    MetadataRepository metadataRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    def "Test getting all dynamic profiles"() {
        given:

        Folder myFirstFolder = folderRepository.save(new Folder(
                label: "My first Folder"
        ))

        when:
        DataModel testDataModel = DataModelBasedProfileTest.testProfileModel
        testDataModel.folder = myFirstFolder

        UUID dataModelId = contentsService.saveWithContent(testDataModel, null).id


        DataModel saved = dataModelCacheableRepository.loadWithContent(dataModelId)


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

        contentsService.saveWithContent(testDataModel).id

        List<Profile> profiles = dynamicProfileService.getDynamicProfiles()
        Profile dynamicProfile = profiles[0]

        then:


        metadataRepository.getNamespaceKeys() == [
                "org.maurodata.profile": ["metadataNamespace","profileApplicableForDomains"] as Set,
                "org.maurodata.profile.dataelement":["metadataPropertyName", "regularExpression"] as Set
        ]

    }

}