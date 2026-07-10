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

    @JsonIgnore
    AppliedProfileSection parentSection

    AppliedProfileSection() {}

    List<AppliedProfileField> fields = []

    List<AppliedProfileSection> sections = []

    @Override
    String getLabel() {
        return sourceProfileSection?.getLabel() ?: super.label
    }

    @Override
    String getDescription() {
        return sourceProfileSection.getDescription()
    }

    AppliedProfileSection(ProfileSection profileSection, AppliedProfile parentProfile, AdministeredItem administeredItem, AppliedProfileSection parentSection = null) {
        this.sourceProfileSection = profileSection
        this.administeredItem = administeredItem
        this.parentProfile = parentProfile
        this.parentSection = parentSection
        this.sections = profileSection.sections.collect {
            new AppliedProfileSection(it, this.parentProfile, administeredItem, this)
        }
        this.fields = profileSection.fields.collect {
            new AppliedProfileField(it, this, administeredItem)
        }
    }

    @CompileDynamic
    AppliedProfileSection(ProfileSection profileSection, AppliedProfile parentProfile, Map sectionBody, AppliedProfileSection parentSection = null) {
        this.sourceProfileSection = profileSection
        this.parentProfile = parentProfile
        this.parentSection = parentSection
        profileSection.sections.each {childProfileSection ->
            Map appliedSection = sectionBody["sections"].find {
                (it['name'] == childProfileSection.name)
            } as Map
            if(appliedSection) {
                this.sections.add(new AppliedProfileSection(childProfileSection, this.parentProfile, appliedSection, this))
            }
        }
        profileSection.fields.each {profileField ->
            Map appliedField = sectionBody["fields"].find {
                (profileField.metadataPropertyName && it['metadataPropertyName'] == profileField.metadataPropertyName) ||
                (!profileField.metadataPropertyName && it['fieldName'] == profileField.fieldName)
            } as Map
            if(appliedField) {
                this.fields.add(new AppliedProfileField(profileField, this, appliedField))
            }
        }
    }


}
