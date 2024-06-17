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

    String currentValue

    Boolean derived
    Boolean uneditable
    Boolean editableAfterFinalisation
    String derivedFrom


}
