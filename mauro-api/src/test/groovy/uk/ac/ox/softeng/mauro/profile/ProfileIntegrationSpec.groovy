package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfile
import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfileField
import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfileSection

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.profile.MetadataNamespaceDTO
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.profile.test.DataModelBasedProfileTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
class ProfileIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID profileDataModelId

    @Shared
    UUID appliedProfileDataModelId

    void setupSpec() {
        Folder folderResponse = (Folder) POST('/folders', [label: 'Test folder'], Folder)
        profileDataModelId = importDataModel(DataModelBasedProfileTest.testProfileModel, folderResponse)
        appliedProfileDataModelId = importDataModel(DataModelBasedProfileTest.testAppliedProfileModel, folderResponse)
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

        List<MetadataNamespaceDTO> namespaces = (List<MetadataNamespaceDTO>) GET('/metadata/namespaces', List<MetadataNamespaceDTO>)

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
        List<Profile> unusedProfileResponse = (List<Profile>) GET("/dataModels/$modelId/profiles/unused", List<Profile>)
        unusedProfileResponse.metadataNamespace == unusedProfiles
        List<Profile> usedProfileResponse = (List<Profile>) GET("/dataModels/$modelId/profiles/used", List<Profile>)
        usedProfileResponse.metadataNamespace == usedProfiles

        where:

        modelId                   | unusedProfiles                          | usedProfiles
        profileDataModelId        | ["com.test.assets"]                     | [ProfileSpecificationProfile.NAMESPACE]
        appliedProfileDataModelId | [ProfileSpecificationProfile.NAMESPACE] | ["com.test.assets"]

    }

    void "get profile details"() {
        when:
        String profileNamespace = "uk.ac.ox.softeng.mauro.profile"
        String profileName = DataModelBasedProfileTest.testProfileModelName.replace(" ", "%20")
        String profileVersion = "1.0.0"
        Map profileDetails = GET("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion")
        then:

        !profileDetails.errors
        expectedPropertyMap.every {key, value ->
            profileDetails.sections.fields.flatten().find {field ->
                field.metadataPropertyName == key &&
                field.currentValue == value &&
                !field.errors
            }
        }
    }

    void "test validate profile"() {
        when:
        String profileNamespace = "uk.ac.ox.softeng.mauro.profile"
        String profileName = DataModelBasedProfileTest.testProfileModelName.replace(" ", "%20")
        String profileVersion = "1.0.0"
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

        Map responseMap = POST("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion/validate", requestBody)

        then:
        !responseMap.errors
        responseMap.sections.fields.flatten().every {
            !it.errors
        }

        when:
        requestBody.sections[1].fields[0].currentValue = "Test"
        responseMap = POST("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion/validate", requestBody)
        then:
        !responseMap.errors
        responseMap.sections.fields.flatten().find {
            it.metadataPropertyName == 'size' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[1].currentValue = "High"
        responseMap = POST("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion/validate", requestBody)
        then:
        !responseMap.errors
        responseMap.sections.fields.flatten().find {
            it.metadataPropertyName == 'priority' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[2].currentValue = "test@test@test"
        responseMap = POST("/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion/validate", requestBody)
        then:
        !responseMap.errors
        responseMap.sections.fields.flatten().find {
            it.metadataPropertyName == 'contactEmail' &&
            it.errors.size() == 1
        }

    }
}