package uk.ac.ox.softeng.mauro.profile.applied

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.profile.Profile

class AppliedProfile implements Profile {

    private AdministeredItem administeredItem
    private Profile sourceProfile

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


    AppliedProfile(Profile profile, Map profileBody) {
        this.sourceProfile = profile
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
        if (!profileApplicableForDomains.contains(administeredItem.class.simpleName)) {
            errors.add(
                "The profile '${displayName}' cannot be applied to an object of type '${administeredItem.class.simpleName}'.  Allowed types are $profileApplicableForDomains'")
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
