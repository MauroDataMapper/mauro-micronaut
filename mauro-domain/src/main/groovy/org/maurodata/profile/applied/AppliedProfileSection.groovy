package org.maurodata.profile.applied

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.profile.ProfileSection

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class AppliedProfileSection extends ProfileSection {

    private ProfileSection sourceProfileSection

    @JsonIgnore
    private AdministeredItem administeredItem

    @JsonIgnore
    AppliedProfile parentProfile

    AppliedProfileSection() {}

    List<AppliedProfileField> fields = []

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

    @CompileDynamic
    AppliedProfileSection(ProfileSection profileSection, AppliedProfile parentProfile, Map sectionBody) {
        this.sourceProfileSection = profileSection
        this.parentProfile = parentProfile
        this.fields = profileSection.fields.collect {profileField ->
            new AppliedProfileField(profileField, this,
                                    sectionBody["fields"].find { it['metadataPropertyName'] == profileField.metadataPropertyName } as Map)
        }
    }


}
