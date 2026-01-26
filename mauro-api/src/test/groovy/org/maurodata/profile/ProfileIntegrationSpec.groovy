package org.maurodata.profile

import org.maurodata.api.profile.dto.MetadataNamespaceDTO
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Metadata
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.testing.CommonDataSpec
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.profile.test.DataModelBasedProfileTest

import jakarta.inject.Singleton
import org.maurodata.web.ListResponse
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
class ProfileIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID profileDataModelId

    @Shared
    UUID appliedProfileDataModelId

    @Shared
    UUID folderId

    static String profileNamespace = 'org.maurodata.profile'
    static String profileName = DataModelBasedProfileTest.testProfileModelName
    static String profileVersion = '1.0.0'


    void setupSpec() {
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        folderId = folderResponse.id
        profileDataModelId = importDataModel(DataModelBasedProfileTest.testProfileModel, folderResponse)
        appliedProfileDataModelId = importDataModel(DataModelBasedProfileTest.testAppliedProfileModel, folderResponse)
    }

    static Map expectedPropertyMap = [
        'size'                       : '1.5',
        'priority'                   : '3',
        'contactEmail'               : 'test@test.com',
        'retired'                    : 'true',
        'createdDate'                : '2024-05-01',
        // 'Asset Creation/Deleted date': '2024-06-01' // Ignore this one for testing for now
    ]


    void 'get namespaces'() {
        when:

        List<MetadataNamespaceDTO> namespaces = profileApi.getNamespaces(null)

        then:
        namespaces.size() == 3
        namespaces.find {it.namespace == ProfileSpecificationFieldProfile.NAMESPACE}
        namespaces.find {it.namespace == ProfileSpecificationProfile.NAMESPACE}

        namespaces.find {it.namespace == 'com.test.assets'}.keys == [
            'Asset Creation/Deleted date',
            'Asset Creation/Expiry date',
            'contactEmail',
            'createdDate',
            'priority',
            'retired',
            'size'
        ]
    }

    @Unroll
    void 'get used and unused profiles'() {

        expect:
        List<MauroPluginDTO> unusedProfileResponse = profileApi.getUnusedProfiles('dataModel', modelId)
        unusedProfileResponse.metadataNamespace == unusedProfiles
        List<MauroPluginDTO> usedProfileResponse = profileApi.getUsedProfiles('dataModel', modelId)
        usedProfileResponse.metadataNamespace == usedProfiles

        where:

        modelId                   | unusedProfiles                          | usedProfiles
        profileDataModelId        | ['com.test.assets']                     | [ProfileSpecificationProfile.NAMESPACE]
        appliedProfileDataModelId | [ProfileSpecificationProfile.NAMESPACE] | ['com.test.assets']

    }

    void 'get profile details'() {
        when:
        //Map profileDetails1 = GET('/dataModel/$appliedProfileDataModelId/profile/$profileNamespace/$profileName/$profileVersion')
        AppliedProfile profileDetails = profileApi.getProfiledItem(
            'dataModel', appliedProfileDataModelId, profileNamespace, profileName, profileVersion)
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

    void 'test validate profile'() {
        when:
        Map requestBody =
            [sections: [
                [name       : 'Asset Creation',
                 description: 'Details of when an asset was created',
                 fields     : [[
                                   fieldName           : 'Created date',
                                   metadataPropertyName: 'createdDate',
                                   currentValue        : '2024-05-01'
                               ], [
                                   fieldName   : 'Deleted date',
                                   currentValue: '2024-06-01'
                               ]
                     // Leave out expiry date as a test
                                ]],
                [name       : 'Asset details',
                 description: 'Details of an asset',
                 fields     : [[
                                   fieldName           : 'Size',
                                   metadataPropertyName: 'size',
                                   currentValue        : '1.5'
                               ], [
                                   fieldName           : 'Priority',
                                   metadataPropertyName: 'priority',
                                   currentValue        : '3'
                               ], [
                                   fieldName           : 'Contact email address',
                                   metadataPropertyName: 'contactEmail',
                                   currentValue        : 'test@test.com'
                               ], [
                                   fieldName           : 'Is retired',
                                   metadataPropertyName: 'retired',
                                   currentValue        : 'true'
                               ]
                 ]
                ]
            ]]

        AppliedProfile appliedProfile = profileApi.validateProfile(
            'dataModel', appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)

        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().every {
            !it.errors
        }

        when:
        requestBody.sections[1].fields[0].currentValue = 'Test'
        appliedProfile = profileApi.validateProfile(
            'dataModel', appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'size' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[1].currentValue = 'High'
        appliedProfile = profileApi.validateProfile(
            'dataModel', appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'priority' &&
            it.errors.size() == 1
        }

        when:
        requestBody.sections[1].fields[2].currentValue = 'test@test@test'
        appliedProfile = profileApi.validateProfile(
            'dataModel', appliedProfileDataModelId, profileNamespace, profileName, profileVersion, requestBody)
        then:
        !appliedProfile.errors
        appliedProfile.sections.fields.flatten().find {
            it.metadataPropertyName == 'contactEmail' &&
            it.errors.size() == 1
        }

    }

    void 'apply profile' () {
        when:
        Map requestBody =
            [sections: [
                [name       : 'Asset Creation',
                 description: 'Details of when an asset was created',
                 fields     : [[
                                   fieldName           : 'Created date',
                                   metadataPropertyName: 'createdDate',
                                   currentValue        : '2024-05-01'
                               ], [
                                   fieldName   : 'Deleted date',
                                   currentValue: '2024-06-01'
                 ], [
                                   fieldName   : 'Expiry date',
                                   currentValue: '2024-07-01'
                               ]]],
                [name       : 'Asset details',
                 description: 'Details of an asset',
                 fields     : [[
                                   fieldName           : 'Size',
                                   metadataPropertyName: 'size',
                                   currentValue        : '1.5'
                               ], [
                                   fieldName           : 'Priority',
                                   metadataPropertyName: 'priority',
                                   currentValue        : '3'
                               ], [
                                   fieldName           : 'Contact email address',
                                   metadataPropertyName: 'contactEmail',
                                   currentValue        : 'test@test.com'
                               ], [
                                   fieldName           : 'Is retired',
                                   metadataPropertyName: 'retired',
                                   currentValue        : 'true'
                               ]
                 ]
                ]
            ]]


        DataModel newDataModel = dataModelApi.create(folderId, new DataModel(label: 'My new data model'))
        profileApi.applyProfile('dataModel', newDataModel.id, profileNamespace, profileName, profileVersion, requestBody)
        AppliedProfile appliedProfile = profileApi.getProfiledItem('dataModel', newDataModel.id, profileNamespace, profileName, profileVersion)
        then:
        appliedProfile.sections.find {it.label == 'Asset Creation'}.fields.find{it.fieldName == 'Created date'}.currentValue == '2024-05-01'
        appliedProfile.sections.find {it.label == 'Asset Creation'}.fields.find{it.fieldName == 'Deleted date'}.currentValue == '2024-06-01'
        appliedProfile.sections.find {it.label == 'Asset Creation'}.fields.find{it.fieldName == 'Expiry date'}.currentValue == '2024-07-01'

        appliedProfile.sections.find {it.label == 'Asset details'}.fields.find{it.fieldName == 'Size'}.currentValue == '1.5'
        appliedProfile.sections.find {it.label == 'Asset details'}.fields.find{it.fieldName == 'Priority'}.currentValue == '3'
        appliedProfile.sections.find {it.label == 'Asset details'}.fields.find{it.fieldName == 'Contact email address'}.currentValue == 'test@test.com'
        appliedProfile.sections.find {it.label == 'Asset details'}.fields.find{it.fieldName == 'Is retired'}.currentValue == 'true'

        when:
        ListResponse<Metadata> metadata = metadataApi.list('dataModel', newDataModel.id)
        then:
        metadata.count == 7
        metadata.items.every {it.namespace == 'com.test.assets' }
        metadata.items.find {it.key == 'createdDate'}.value == '2024-05-01'
        metadata.items.find {it.key == 'Asset Creation/Deleted date'}.value == '2024-06-01'
        metadata.items.find {it.key == 'Asset Creation/Expiry date'}.value == '2024-07-01'
        metadata.items.find {it.key == 'size'}.value == '1.5'
        metadata.items.find {it.key == 'priority'}.value == '3'
        metadata.items.find {it.key == 'contactEmail'}.value == 'test@test.com'
        metadata.items.find {it.key == 'retired'}.value == 'true'

    }
}