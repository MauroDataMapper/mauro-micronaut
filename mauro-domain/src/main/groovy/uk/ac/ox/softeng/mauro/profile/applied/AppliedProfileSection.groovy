package uk.ac.ox.softeng.mauro.profile.applied

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.profile.ProfileSection

class AppliedProfileSection extends ProfileSection {

    private ProfileSection sourceProfileSection
    private AdministeredItem administeredItem
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


}
