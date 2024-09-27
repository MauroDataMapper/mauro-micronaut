package uk.ac.ox.softeng.mauro.test.profile

import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfile

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationProfile

@MicronautTest
class JsonProfileSpec extends Specification {

    @Inject
    ProfileService profileService

    @Inject
    MauroPluginService mauroPluginService

    def "test getting all static profiles"() {
        expect:
        mauroPluginService.listPlugins(Profile).size() == 2
        profileService.staticProfiles.size() == 2

        profileService.staticProfiles.find { it.displayName == "Profile Specification Profile" }
        profileService.staticProfiles.find { it.displayName == "Profile Specification Field Profile" }
    }


    def "test construction of json profile"() {
        when:
        Profile p = profileService.staticProfiles.find { it.displayName == "Profile Specification Profile" }

        then:
        p.metadataNamespace == ProfileSpecificationProfile.NAMESPACE
        p.profileApplicableForDomains == ["DataModel"]
        p.sections.size() == 1
        p.sections.first().label == "Profile Specification"
        p.sections.first().fields.size() == 3
        p.sections.first().fields.first().fieldName == "Metadata namespace"
    }


    def "test validating an item against a profile - success"() {

        when:
        Profile profile = profileService.staticProfiles.find { it.displayName == "Profile Specification Profile" }

        DataModel dm = DataModel.build {
            label "My first profiled data model"
            description "Hope this one works!"
            metadata(ProfileSpecificationProfile.NAMESPACE,
                    ["metadataNamespace": "com.test"])
        }
        then:
        new AppliedProfile(profile, dm).collateErrors() == []

        when:
        dm.metadata(ProfileSpecificationProfile.NAMESPACE,
                ["domainsApplicable"        : "DataModel;DataElement",
                 "editableAfterFinalisation": "true"])

        then:
        new AppliedProfile(profile, dm).collateErrors() == []
    }

    def "test validating an item against a profile - failure"() {

        when:
        Profile profile = profileService.staticProfiles.find { it.displayName == "Profile Specification Profile" }

        Terminology terminology = Terminology.build {
            label "My first profiled terminology"
            description "Hope this one works!"
            metadata(ProfileSpecificationProfile.NAMESPACE,
                    ["metadataNamespace": "com.test"])
        }
        List<String> errors = new AppliedProfile(profile, terminology).collateErrors()
        then:
        errors.size() == 1
        errors.first().contains("cannot be applied to an object of type")

        when:
        DataModel dm = DataModel.build {
            label "My first profiled data model"
            description "Hope this one works!"
            metadata(ProfileSpecificationProfile.NAMESPACE,
                    ["metadataNamespace"        : "com.test",
                     "domainsApplicable"        : "DataModel;DataElement",
                     "editableAfterFinalisation": "something"])
        }
        errors = new AppliedProfile(profile, dm).collateErrors()

        then:
        errors.size() == 1
        errors.first().contains("does not match specified data type")

    }

    def "test get profile keys"() {
        when:
        Profile profile = profileService.staticProfiles.find { it.displayName == "Profile Specification Profile" }

        then:
        profile.getKeys() == ["domainsApplicable", "editableAfterFinalisation", "metadataNamespace"]

        when:
        profile = profileService.staticProfiles.find { it.displayName == "Profile Specification Field Profile" }

        then:
        profile.getKeys() == ["defaultValue", "editableAfterFinalisation", "metadataPropertyName", "regularExpression"]

    }
}