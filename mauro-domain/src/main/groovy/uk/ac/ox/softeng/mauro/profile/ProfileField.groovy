package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

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
        getMetadataPropertyName()?: "${sectionName}/${getFieldName()}"
    }



}
