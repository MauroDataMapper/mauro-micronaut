package org.maurodata.profile

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfileField {

    String fieldName
    String metadataPropertyName
    String description
    Integer maxMultiplicity
    Integer minMultiplicity
    Map<String, String> allowedValues
    String regularExpression
    String defaultValue
    UUID referenceTerminology

    ProfileFieldDataType dataType

    Boolean derived
    Boolean uneditable
    Boolean editableAfterFinalisation
    String derivedFrom


    String getMetadataKey(String sectionName) {
        getMetadataPropertyName()?: "${sectionName}/${getFieldName()}"
    }



}
