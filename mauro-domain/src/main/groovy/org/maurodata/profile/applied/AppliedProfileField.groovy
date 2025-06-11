package org.maurodata.profile.applied

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.profile.ProfileField
import org.maurodata.profile.ProfileFieldDataType
import org.maurodata.profile.ProfileSection

import com.fasterxml.jackson.annotation.JsonIgnore

class AppliedProfileField extends ProfileField {


    private ProfileField sourceProfileField

    @JsonIgnore
    private AdministeredItem administeredItem

    @JsonIgnore
    AppliedProfileSection parentSection

    String currentValue

    List<String> errors = []

    @Override
    String getMetadataKey(String sectionName) {
        getMetadataPropertyName()?: "${sectionName}/${getFieldName()}"
    }

    AppliedProfileField() {}

    AppliedProfileField(ProfileField profileField, AppliedProfileSection parentSection, AdministeredItem administeredItem) {
        this.sourceProfileField = profileField
        this.administeredItem = administeredItem
        this.fieldName = profileField.fieldName
        this.metadataPropertyName = profileField.metadataPropertyName
        this.parentSection = parentSection
        this.currentValue = administeredItem.metadata?.find {
            it.namespace == parentSection.parentProfile.metadataNamespace && it.key == getMetadataKey(parentSection.label)
        }?.value
        this.description = profileField.description
        this.maxMultiplicity = profileField.maxMultiplicity
        this.minMultiplicity = profileField.minMultiplicity
        this.allowedValues = profileField.allowedValues
        this.regularExpression = profileField.regularExpression
        this.defaultValue = profileField.defaultValue
        this.dataType = profileField.dataType
        this.derived = profileField.derived
        this.uneditable = profileField.uneditable
        this.editableAfterFinalisation = profileField.editableAfterFinalisation
        this.derivedFrom = profileField.derivedFrom
        validate()
    }

    AppliedProfileField(ProfileField profileField, AppliedProfileSection parentSection, Map fieldBody) {
        this.sourceProfileField = profileField
        this.parentSection = parentSection
        this.currentValue = fieldBody.currentValue
        this.fieldName = profileField.fieldName
        this.description = profileField.description
        this.metadataPropertyName = profileField.metadataPropertyName
        this.maxMultiplicity = profileField.maxMultiplicity
        this.minMultiplicity = profileField.minMultiplicity
        this.allowedValues = profileField.allowedValues
        this.regularExpression = profileField.regularExpression
        this.defaultValue = profileField.defaultValue
        this.dataType = profileField.dataType
        this.derived = profileField.derived
        this.uneditable = profileField.uneditable
        this.editableAfterFinalisation = profileField.editableAfterFinalisation
        this.derivedFrom = profileField.derivedFrom
        validate()
    }

    void validate() {

        if(currentValue && !dataType.validateStringAgainstType(currentValue)) {
            errors.add ("Value '$currentValue' does not match specified data type: ${dataType.label}".toString())
        }
        if(currentValue && allowedValues && !allowedValues.contains(currentValue)) {
            errors.add("Value '$currentValue' should be one of the provided values: $allowedValues".toString())
        }
        if(currentValue && regularExpression && !currentValue.matches(regularExpression)) {
            errors.add("Value '$currentValue' should match the provided regular expression".toString())
        }
        if(!currentValue && minMultiplicity > 0) {
            errors.add("A value for field '$fieldName' must be provided".toString())
        }
        // TODO: Something about Max Multiplicity
    }



}
