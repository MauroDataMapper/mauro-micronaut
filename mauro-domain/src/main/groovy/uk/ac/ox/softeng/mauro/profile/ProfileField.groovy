package uk.ac.ox.softeng.mauro.profile

class ProfileField {

    String fieldName
    String metadataPropertyName
    String description
    Integer maxMultiplicity
    Integer minMultiplicity
    List<String> allowedValues
    String regularExpression
    String defaultValue

    ProfileFieldDataType dataType

    Boolean derived
    Boolean uneditable
    Boolean editableAfterFinalisation
    String derivedFrom


    String getMetadataKey(String sectionName) {
        metadataPropertyName?: "${sectionName}/${fieldName}"
    }

    List<String> validate(String value) {
        List<String> errors = []
        if(value && !dataType.validateStringAgainstType(value)) {
            errors.add ("Value '$value' does not match specified data type: ${dataType.label}")
        }
        if(value && allowedValues && !allowedValues.contains(value)) {
            errors.add("Value '$value' should be one of the provided values: $allowedValues")
        }
        if(value && regularExpression && !value.matches(regularExpression)) {
            errors.add("Value '$value' should match the provided regular expression")
        }
        if(!value && minMultiplicity > 0) {
            errors.add("A value for field '$fieldName' must be provided")
        }
        // TODO: Something about Max Multiplicity
        return errors
    }

}
