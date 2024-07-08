package uk.ac.ox.softeng.mauro.profile.test

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationFieldProfile
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationProfile

class DataModelBasedProfileTest {

    static DataModel testProfileModel = DataModel.build {
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


}
