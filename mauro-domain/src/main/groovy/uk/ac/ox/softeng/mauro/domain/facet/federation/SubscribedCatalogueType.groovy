package uk.ac.ox.softeng.mauro.domain.facet.federation

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import groovy.transform.CompileStatic

@CompileStatic
enum SubscribedCatalogueType {
    MAURO_JSON('Mauro JSON'),
    ATOM('Atom');

    @JsonAlias(['subscribed_catalogue_type'])
    private final String label;

    private static Map<String, SubscribedCatalogueType> LOOKUP_BY_LABEL =
       values().collectEntries {[ standardizeLabelStringCaseAndWhitespace(it.label), it]}

    SubscribedCatalogueType(String label) {
        this.label = label;
    }

    @JsonCreator
    // This is the factory method and must be static
    static SubscribedCatalogueType fromString(String label) {
        return Optional
            .ofNullable(LOOKUP_BY_LABEL.get(standardizeLabelStringCaseAndWhitespace(label)))
            .orElseThrow(() -> new IllegalArgumentException(label));
    }

    static String standardizeLabelStringCaseAndWhitespace(String label) {
        label.toUpperCase().replaceAll(/ /, "_")
    }

    static List<String> labels() {
        LOOKUP_BY_LABEL.keySet().sort()
    }
}

