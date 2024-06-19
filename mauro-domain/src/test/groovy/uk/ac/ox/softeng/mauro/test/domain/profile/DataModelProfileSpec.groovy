package uk.ac.ox.softeng.mauro.test.domain.profile

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationFieldProfile
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationProfile

@MicronautTest
class DataModelProfileSpec extends Specification {

    @Inject
    ProfileService profileService

    @Inject
    MauroPluginService mauroPluginService

    @Shared
    DataModel testProfileModel = DataModel.build {
        label "Asset management profile"
        description "Details pertaining to the management of data assets"
        metadata(ProfileSpecificationProfile.NAMESPACE,
                ["metadataNamespace": "com.test.assets",
                 "profileApplicableForDomains": "DataModel; Terminology"])
        primitiveType {
            label "Decimal"
        }
        primitiveType {
            label "Date"
        }
        primitiveType {
            label "String"
        }
        primitiveType {
            label "Boolean"
        }
        enumerationType {
            label "Priority"
            enumerationValue {
                key "1"
                value "High"
            }
            enumerationValue {
                key "2"
                value "Medium"
            }
            enumerationValue {
                key "3"
                value "Low"
            }
        }
        dataClass {
            label "Asset details"
            description "Details of an asset"
            dataElement {
                label "Size"
                description "Size of the asset (in TB)"
                dataType "Decimal"
                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
                        ["metadataPropertyName": "size"])
            }
            dataElement {
                label "Priority"
                description "Priority of the asset (High / Medium / Low)"
                dataType "Priority"
                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
                        ["metadataPropertyName": "priority"])
            }
            dataElement {
                label "Contact email address"
                description "Email address of the contact for this asset"
                dataType "String"
                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
                        ["metadataPropertyName": "contactEmail",
                        "regularExpression": "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$"])
            }
            dataElement {
                label "Is retired"
                description "Whether or not this asset has been retired"
                dataType "Boolean"
                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
                        ["metadataPropertyName": "retired"])
            }
        }
        dataClass {
            label "Asset Creation"
            description "Details of when an asset was created"
            dataElement {
                label "Created date"
                description "Date the asset was created"
                dataType "Date"
                minMultiplicity 1
                maxMultiplicity 1
                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
                        ["metadataPropertyName": "createdDate"])
            }
            dataElement {
                label "Deleted date"
                description "Date the asset was deleted"
                dataType "Date"
                // Test one without creating a metadata property name and having it generated automatically
//                metadata(ProfileSpecificationFieldProfile.NAMESPACE,
//                        ["metadataPropertyName": "createdDate"])
            }
        }
    }


    def "test construction of datamodel profile"() {
        when:
        Profile dynamicProfile = new DataModelBasedProfile(testProfileModel)

        then:
        dynamicProfile.displayName == "Asset management profile"
        dynamicProfile.metadataNamespace == "com.test.assets"
        dynamicProfile.profileApplicableForDomains == ["DataModel", "Terminology"]
        dynamicProfile.sections.size() == 2
        dynamicProfile.sections.first().label == "Asset details"
        dynamicProfile.sections.first().fields.size() == 4
        dynamicProfile.sections.first().fields.first().fieldName == "Size"
        dynamicProfile.sections.first().fields.first().metadataPropertyName == "size"
        dynamicProfile.sections.last().label == "Asset Creation"
        dynamicProfile.sections.last().fields.size() == 2
        dynamicProfile.sections.last().fields.first().fieldName == "Created date"
        dynamicProfile.sections.first().fields.first().getMetadataKey("Asset Creation") == "size"
    }

    def "test validating an item against a dynamic profile - success"() {
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
        Profile dynamicProfile = new DataModelBasedProfile(testProfileModel)

        then:
        dynamicProfile.validate(dataModel) == []

    }

    def "test validating an item against a dynamic profile - failure"() {
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
        Profile dynamicProfile = new DataModelBasedProfile(testProfileModel)
        List<String> errors = dynamicProfile.validate(dataClass)

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
        errors = dynamicProfile.validate(dataModel)

        then:
        errors.size() == 6
        errors.find { it == "Value 'abc' does not match specified data type: decimal" }
        errors.find { it == "Value 'High' should be one of the provided values: [1, 2, 3]" }
        errors.find { it == "Value 'myAssetOwner@notAnEmailAddress@test.com' should match the provided regular expression" }
        errors.find { it == "Value 'not true' does not match specified data type: boolean" }
        errors.find { it == "A value for field 'Created date' must be provided" }
        errors.find { it == "Value '19/19/2024' does not match specified data type: date" }
    }

}
