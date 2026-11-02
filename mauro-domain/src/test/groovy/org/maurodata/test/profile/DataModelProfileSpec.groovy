package org.maurodata.test.profile

import org.maurodata.profile.ProfileField
import org.maurodata.profile.ProfileSection
import org.maurodata.profile.applied.AppliedProfile

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.plugin.MauroPluginService
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.Profile
import org.maurodata.profile.test.DataModelBasedProfileTest

@MicronautTest
class DataModelProfileSpec extends Specification {

    @Inject
    MauroPluginService mauroPluginService

    void "test construction of datamodel profile"() {
        when:
        Profile dynamicProfile = new DataModelBasedProfile(DataModelBasedProfileTest.testProfileModel)

        then:
        dynamicProfile.displayName == "Asset management profile"
        dynamicProfile.metadataNamespace == "com.test.assets"
        dynamicProfile.profileApplicableForDomains == ["DataModel", "Terminology"]
        dynamicProfile.sections.size() == 2

        when:
        ProfileSection profileSection = dynamicProfile.sections.find {it.label == "Asset details"}

        then:
        profileSection.fields.size() == 4

        when:
        ProfileField profileField = profileSection.fields.find {it.fieldName == "Size" }

        then:
        profileField.metadataPropertyName == "size"

        when:
        profileSection = dynamicProfile.sections.find {it.label == "Asset Creation"}

        then:
        profileSection.label == "Asset Creation"
        profileSection.fields.size() == 3

        when:
        profileField = profileSection.fields.find {it.fieldName == "Created date" }

        then:
        profileField.getMetadataKey("Asset Creation") == "createdDate"

    }

    void "test validating an item against a dynamic profile - success"() {
        when:
        DataModel dataModel = DataModel.build {
            label "My first asset"
            description "An asset to test validating against a profile"
            metadata("com.test.assets", [
                   "size"        : "21.5",
                   "priority"    : "1",
                   "contactEmail": "myAssetOwner@test.com",
                   "retired"     : "false",
                   "createdDate" : "19/06/2024",
                   "Asset Creation/Deleted date" : "19/06/2024",
                   "not-part-of-profile": "Something" // This bonus value won't be included in validation
            ])
        }
        Profile dynamicProfile = new DataModelBasedProfile(DataModelBasedProfileTest.testProfileModel)

        then:
        new AppliedProfile(dynamicProfile, dataModel).collateErrors() == []


    }

    void "test validating an item against a dynamic profile - failure"() {
        when:
        DataClass dataClass = DataClass.build {
            label "My first asset"
            description "An asset to test validating against a profile"
            metadata("com.test.assets", [
                    "size"        : "21.5",
                    "priority"    : "1",
                    "contactEmail": "myAssetOwner@test.com",
                    "retired"     : "false",
                    "createdDate" : "19/06/2024",
                    "Asset Creation/Deleted date" : "19/06/2024"
            ])
        }
        Profile dynamicProfile = new DataModelBasedProfile(DataModelBasedProfileTest.testProfileModel)
        List<String> errors = new AppliedProfile(dynamicProfile, dataClass).collateErrors()

        then:
        errors.size() == 1
        errors.first().contains("cannot be applied to an object of type")

        when:
        DataModel dataModel = DataModel.build {
            label "My first asset"
            description "An asset to test validating against a profile"
            metadata("com.test.assets", [
                    "size"        : "abc",
                    "priority"    : "High",
                    "contactEmail": "myAssetOwner@notAnEmailAddress@test.com",
                    "retired"     : "not true",
                    // "createdDate" : "19/06/2024", // Not set
                    "Asset Creation/Deleted date" : "19/19/2024"
            ])
        }
        errors = new AppliedProfile(dynamicProfile, dataModel).collateErrors()

        then:
        errors.size() == 6
        errors.find { it == "Value 'abc' does not match specified data type: decimal" }
        errors.find { it == "Value 'High' should be one of the provided values: [1, 2, 3]" }
        errors.find { it == "Value 'myAssetOwner@notAnEmailAddress@test.com' should match the provided regular expression" }
        errors.find { it == "Value 'not true' does not match specified data type: boolean" }
        errors.find { it == "A value for field 'Created date' must be provided" }
        errors.find { it == "Value '19/19/2024' does not match specified data type: date" }
    }

    void "test get profile keys"() {
        when:
        Profile profile = new DataModelBasedProfile(DataModelBasedProfileTest.testProfileModel)

        then:
        profile.getKeys().sort() == ["Asset Creation/Deleted date", "Asset Creation/Expiry date", "contactEmail", "createdDate", "priority", "retired", "size"]

    }

}
