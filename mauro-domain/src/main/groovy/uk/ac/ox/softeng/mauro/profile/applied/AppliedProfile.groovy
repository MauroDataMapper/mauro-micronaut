package uk.ac.ox.softeng.mauro.profile.applied

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.plugin.MauroPluginDTO
import uk.ac.ox.softeng.mauro.profile.Profile

class AppliedProfile extends MauroPluginDTO {

    @JsonIgnore
    private AdministeredItem administeredItem

    @JsonIgnore
    private Profile sourceProfile

    List<AppliedProfileSection> sections

    @Override
    String getMetadataNamespace() {
        return sourceProfile.metadataNamespace
    }

    @Override
    String getName() {
        return sourceProfile.getName()
    }

    @Override
    String getDisplayName() {
        return sourceProfile.getDisplayName()
    }

    List<String> getProfileApplicableForDomains() {
        return sourceProfile.profileApplicableForDomains
    }

    List<String> errors = []

    // Empty constructor for JSON deserialisation
    AppliedProfile() { }

    AppliedProfile(Profile profile, AdministeredItem administeredItem, Map profileBody) {
        this.sourceProfile = profile
        this.administeredItem = administeredItem
        this.sections = profile.sections.collect {profileSection ->
            new AppliedProfileSection(profileSection, this,
                                      profileBody["sections"].find { it.name == profileSection.label } as Map)
        }
        validate()
    }

    AppliedProfile(Profile profile, AdministeredItem administeredItem) {
        this.sourceProfile = profile
        this.administeredItem = administeredItem
        this.sections = profile.sections.collect {
            new AppliedProfileSection(it, this, administeredItem)
        }
        validate()
    }


    void validate() {
        String typeName = administeredItem.getDomainType()
        if (!sourceProfile.isApplicableForDomain(typeName)) {
            errors.add(
                "The profile '${displayName}' cannot be applied to an object of type '${administeredItem.class.simpleName}'.  Allowed types are $profileApplicableForDomains'".toString())
        }
    }

    /*
        This is a convenience method for testing
     */
    List<String> collateErrors() {
        List<String> returnErrors = []
        returnErrors.addAll(errors)

        sections.each { section ->
            ((AppliedProfileSection) section).fields.each {field ->
                returnErrors.addAll(((AppliedProfileField) field).errors)
            }
        }
        return returnErrors
    }


}
