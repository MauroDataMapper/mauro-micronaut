package org.maurodata.domain.facet.federation

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import groovy.transform.CompileStatic

@CompileStatic
enum SubscribedCatalogueAuthenticationType {
    OAUTH_CLIENT_CREDENTIALS('OAuth (Client Credentials)'),
    API_KEY('API Key'),
    NO_AUTHENTICATION('No Authentication')

    @JsonAlias(['subscribed_catalogue_authentication_type'])
    private final String label

    private static final Map<String, SubscribedCatalogueAuthenticationType> LOOKUP_BY_LABEL =
        values().collectEntries {[SubscribedCatalogueType.standardizeLabelStringCaseAndWhitespace(it.label), it]}

    SubscribedCatalogueAuthenticationType(String label) {
        this.label = label
    }

    @JsonCreator
    // This is the factory method and must be static
    static SubscribedCatalogueAuthenticationType fromString(String label) {
        return Optional
            .ofNullable(LOOKUP_BY_LABEL.get(SubscribedCatalogueType.standardizeLabelStringCaseAndWhitespace(label))?:valueOf(label))
            .orElseThrow(() -> new IllegalArgumentException(label))
    }


    static List<String> labels() {
        LOOKUP_BY_LABEL.keySet().sort()
    }

}
