package uk.ac.ox.softeng.mauro.profile.applied

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.profile.ProfileField
import uk.ac.ox.softeng.mauro.profile.ProfileFieldDataType
import uk.ac.ox.softeng.mauro.profile.ProfileSection

class AppliedProfileField extends ProfileField {

    private ProfileField sourceProfileField
    private AdministeredItem administeredItem
    AppliedProfileSection parentSection

    String currentValue
    List<String> errors = []

    @Override
    String getFieldName() {
        return sourceProfileField.getFieldName()
    }

    @Override
    String getDescription() {
        return sourceProfileField.getDescription()
    }

    @Override
    String getMetadataKey(String sectionName) {
        return sourceProfileField.getMetadataKey(sectionName)
    }

    @Override
    String getMetadataPropertyName() {
        return sourceProfileField.getMetadataPropertyName()
    }

    @Override
    Integer getMaxMultiplicity() {
        return sourceProfileField.getMaxMultiplicity()
    }

    @Override
    Integer getMinMultiplicity() {
        return sourceProfileField.getMinMultiplicity()
    }

    @Override
    List<String> getAllowedValues() {
        return sourceProfileField.getAllowedValues()
    }

    @Override
    String getRegularExpression() {
        return sourceProfileField.getRegularExpression()
    }

    @Override
    String getDefaultValue() {
        return sourceProfileField.getDefaultValue()
    }

    @Override
    ProfileFieldDataType getDataType() {
        return sourceProfileField.getDataType()
    }

    @Override
    Boolean getDerived() {
        return sourceProfileField.getDerived()
    }

    @Override
    Boolean getUneditable() {
        return sourceProfileField.getUneditable()
    }

    @Override
    Boolean getEditableAfterFinalisation() {
        return sourceProfileField.getEditableAfterFinalisation()
    }

    @Override
    String getDerivedFrom() {
        return sourceProfileField.getDerivedFrom()
    }

    AppliedProfileField(ProfileField profileField, AppliedProfileSection parentSection, AdministeredItem administeredItem) {
        this.sourceProfileField = profileField
        this.administeredItem = administeredItem
        this.parentSection = parentSection
        this.currentValue = administeredItem.metadata?.find {
            it.namespace == parentSection.parentProfile.metadataNamespace && it.key == getMetadataKey(parentSection.label)
        }?.value
        validate()
    }

    void validate() {

        if(currentValue && !dataType.validateStringAgainstType(currentValue)) {
            errors.add ("Value '$currentValue' does not match specified data type: ${dataType.label}")
        }
        if(currentValue && allowedValues && !allowedValues.contains(currentValue)) {
            errors.add("Value '$currentValue' should be one of the provided values: $allowedValues")
        }
        if(currentValue && regularExpression && !currentValue.matches(regularExpression)) {
            errors.add("Value '$currentValue' should match the provided regular expression")
        }
        if(!currentValue && minMultiplicity > 0) {
            errors.add("A value for field '$fieldName' must be provided")
        }
        // TODO: Something about Max Multiplicity
    }



}
