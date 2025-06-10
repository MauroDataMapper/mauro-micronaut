package org.maurodata.domain.facet

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

@CompileStatic
@JsonDeserialize(converter = SemanticLinkTypeConverter)
enum SemanticLinkType {

    REFINES('Refines'),
    DOES_NOT_REFINE('Does Not Refine'),
    ABSTRACTS('Abstracts'),
    DOES_NOT_ABSTRACT('Does Not Abstract'),
    IS_FROM('Is From', false)

    String label
    boolean isAssignable

    SemanticLinkType(String label) {
        this.label = label
        this.isAssignable = true
    }

    SemanticLinkType(String label, boolean isAssignable) {
        this.label = label
        this.isAssignable = isAssignable
    }

    static SemanticLinkType semanticLinkTypeForLabel(final String label) {
        valueOf(label.replaceAll(' ', '_').toUpperCase())
    }

    static class SemanticLinkTypeConverter extends StdConverter<String, SemanticLinkType> {

        @Override
        SemanticLinkType convert(String value) {
            valueOf(value.replaceAll(' ', '_').toUpperCase())
        }
    }

}
