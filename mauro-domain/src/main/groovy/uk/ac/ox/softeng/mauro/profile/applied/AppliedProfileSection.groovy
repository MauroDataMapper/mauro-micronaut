package uk.ac.ox.softeng.mauro.profile.applied

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.profile.ProfileSection

import com.fasterxml.jackson.annotation.JsonIgnore

class AppliedProfileSection extends ProfileSection {

    private ProfileSection sourceProfileSection

    @JsonIgnore
    private AdministeredItem administeredItem

    @JsonIgnore
    AppliedProfile parentProfile

    @Override
    String getLabel() {
        return sourceProfileSection.getLabel()
    }

    @Override
    String getDescription() {
        return sourceProfileSection.getDescription()
    }

    AppliedProfileSection(ProfileSection profileSection, AppliedProfile parentProfile, AdministeredItem administeredItem) {
        this.sourceProfileSection = profileSection
        this.administeredItem = administeredItem
        this.parentProfile = parentProfile
        this.fields = profileSection.fields.collect {
            new AppliedProfileField(it, this, administeredItem)
        }
    }

    AppliedProfileSection(ProfileSection profileSection, AppliedProfile parentProfile, Map sectionBody) {
        this.sourceProfileSection = profileSection
        this.parentProfile = parentProfile
        this.fields = profileSection.fields.collect {profileField ->
            new AppliedProfileField(profileField, this,
                                    sectionBody["fields"].find { it.metadataPropertyName == profileField.metadataPropertyName } as Map)
        }
    }


}
