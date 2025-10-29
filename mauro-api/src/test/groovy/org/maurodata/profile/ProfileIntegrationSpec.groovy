package org.maurodata.profile


import io.micronaut.http.client.exceptions.HttpClientResponseException
import org.maurodata.api.profile.MetadataNamespaceDTO
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.testing.CommonDataSpec
import jakarta.inject.Inject

import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.profile.test.DataModelBasedProfileTest

import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
class ProfileIntegrationSpec extends CommonDataSpec {
    static final String PROFILE_PROVIDER_SERVICES = "profileProviderServices"
    static final String PROFILE_NAME = "ProfileSpecificationFieldProfile"
    static final String PROFILE_NAMESPACE = "org.maurodata.profile"
    static final String PROFILE_VERSION = "1.0.0"

    @Shared
    UUID profileDataModelId

    @Shared
    UUID appliedProfileDataModelId

    @Shared
    UUID profileDataElementId

    @Shared
    UUID otherModelId

    @Shared
    UUID otherModelItemId

    @Inject
    @Shared
    DataModelContentRepository dataModelContentRepository

    void setupSpec() {
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        profileDataModelId = importDataModel(DataModelBasedProfileTest.testProfileModel, folderResponse)
        appliedProfileDataModelId = importDataModel(DataModelBasedProfileTest.testAppliedProfileModel, folderResponse)
        DataModel profileDataModel = dataModelContentRepository.findWithContentById(profileDataModelId)
        profileDataElementId = profileDataModel.dataElements.first().id
        DataModel other = dataModelApi.create(folderResponse.id, dataModelPayload())
        otherModelId = other.id
    }

    static Map expectedPropertyMap = [
        "size"                       : "1.5",
        "priority"                   : "3",
        "contactEmail"               : "test@test.com",
        "retired"                    : "true",
        "createdDate"                : "2024-05-01",
        // "Asset Creation/Deleted date": "2024-06-01" // Ignore this one for testing for now
    ]


    void 'get namespaces'() {
        when:

        List<MetadataNamespaceDTO> namespaces = profileApi.getNamespaces(null)

        then:
        namespaces.size() == 3
        namespaces.find {it.namespace == ProfileSpecificationFieldProfile.NAMESPACE}
        namespaces.find {it.namespace == ProfileSpecificationProfile.NAMESPACE}

        namespaces.find {it.namespace == "com.test.assets"}.keys == [
            "Asset Creation/Deleted date",
            "contactEmail",
            "createdDate",
            "priority",
            "retired",
            "size"
        ]
    }

    @Unroll
    void "get used and unused profiles"() {

        expect:
        List<MauroPluginDTO> unusedProfileResponse = profileApi.getUnusedProfiles("dataModel", modelId)
        unusedProfileResponse.metadataNamespace == unusedProfiles
        List<MauroPluginDTO> usedProfileResponse = profileApi.getUsedProfiles("dataModel", modelId)
        usedProfileResponse.metadataNamespace == usedProfiles

        where:

        modelId                   | unusedProfiles                          | usedProfiles
        profileDataModelId        | ["com.test.assets"]                     | [ProfileSpecificationProfile.NAMESPACE]
        appliedProfileDataModelId | [ProfileSpecificationProfile.NAMESPACE] | ["com.test.assets"]

    }

    void "get profile details"() {
        when:
        String profileName = DataModelBasedProfileTest.testProfileModelName
        String profileVersion = PROFILE_VERSION
        //Map profileDetails1 = GET("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion")
        AppliedProfile profileDetails = profileApi.getProfiledItem(
            "dataModel", appliedProfileDataModelId, PROFILE_NAMESPACE, profileName, profileVersion)
        then:

        !profileDetails.errors
        expectedPropertyMap.every {key, value ->
            profileDetails.sections.find {section ->
                section.fields.find {field ->
                    field.metadataPropertyName == key &&
                    field.currentValue == value &&
                    !field.errors
                }
            }
        }
    }


    void "test validate profile"() {
        when:
        String profileNamespace = PROFILE_NAMESPACE
        String profileName = DataModelBasedProfileTest.testProfileModelName
        String profileVersion = PROFILE_VERSION
        Map requestBody =
            [sections: [
                [name       : "Asset Creation",
                 description: "Details of when an asset was created",
                 fields     : [[
                                   fieldName           : "Created date",
                                   metadataPropertyName: "createdDate",
                                   currentValue        : "2024-05-01"
                               ], [
                                   fieldName   : "Deleted date",
                                   currentValue: "2024-06-01"
                               ]]],
                [name       : "Asset details",
                 description: "Details of an asset",
                 fields     : [[
                                   fieldName           : "Size",
                                   metadataPropertyName: "size",
                                   currentValue        : "1.5"
                               ], [
                                   fieldName           : "Priority",
                                   metadataPropertyName: "priority",
                                   currentValue        : "3"
                               ], [
                                   fieldName           : "Contact email address",
                                   metadataPropertyName: "contactEmail",
                                   currentValue        : "test@test.com"
                               ], [
                                   fieldName           : "Is retired",
                                   metadataPropertyName: "retired",
                                   currentValue        : "true"
                               ]
                 ]
                ]
            ]]

        AppliedProfile appliedProfile = profileApi.validateProfile(
            "dataModel", appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)

        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().every {
            !it.errors
        }

        when:
        requestBody.sections[1].fields[0].currentValue = "Test"
        appliedProfile = profileApi.validateProfile(
            "dataModel", appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'size' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[1].currentValue = "High"
        appliedProfile = profileApi.validateProfile(
            "dataModel", appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'priority' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[2].currentValue = "test@test@test"
        appliedProfile = profileApi.validateProfile(
            "dataModel", appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'contactEmail' &&
            it.errors.size() == 1
        }
    }

    void "getMany profiles inside model "() {
        String profileNamespace = PROFILE_NAMESPACE
        String profileName = PROFILE_NAME
        String profileVersion = PROFILE_VERSION
        Map requestPayload = requestPayload(profileDataElementId, profileNamespace, profileName, profileVersion)

        when:
        ProfilesProvidedDTO response = profileApi.getMany("dataModels", profileDataModelId, requestPayload)

        then:
        response
        response.count == 1
        response.profilesProvided[0].profileProviderService["namespace"] == PROFILE_NAMESPACE
        response.profilesProvided[0].profileProviderService["name"] == PROFILE_NAME
        response.profilesProvided[0].profileProviderService["version"] == PROFILE_VERSION
        response.profilesProvided[0].profile.sections['label'] == ["Profile Specification"]
    }

    void "getMany profiles inside model - no profile found -should return empty list"() {
        String profileNamespace = null
        String profileName = 'unknown'
        String profileVersion = '1'
        Map requestPayload = requestPayload(profileDataElementId, profileNamespace, profileName, profileVersion)

        when:
        ProfilesProvidedDTO response = profileApi.getMany("dataModels", profileDataModelId, requestPayload)

        then:
        response
        response.count == 0

    }


    @Unroll
    void 'getMany multiFacetAware #modelItemId inside model #modelId - should throw exception'() {
        given:
        String profileNamespace = PROFILE_NAMESPACE
        String profileName = PROFILE_NAME
        String profileVersion = PROFILE_VERSION
        Map requestPayload = requestPayload(modelItemId, profileNamespace, profileName, profileVersion)

        when:
        ProfilesProvidedDTO response = profileApi.getMany("dataModels", modelId, requestPayload)

        then:
        HttpClientResponseException exception = thrown()
        //not checking exception type too closely. Different exception thrown from test httpclientlibrary vis a vis actual via POSTMAN

        where:
        modelItemId          | modelId
        otherModelId         | profileDataModelId
        profileDataElementId | otherModelId
    }


    protected Map requestPayload(UUID dataElementId, String namespace, String name, String version) {
        Map requestPayload =
            [multiFacetAwareItems: [
                [multiFacetAwareItemDomainType: "dataElement",
                 multiFacetAwareItemId        : dataElementId
                ]
            ]]

        requestPayload[PROFILE_PROVIDER_SERVICES] =
            [[name     : name,
              namespace: namespace,
              version  : version]
            ]
        return requestPayload
    }


}