package org.maurodata.service.search

import groovy.transform.CompileStatic

@CompileStatic
class SemanticSearchAvailability {

    boolean available
    String reason

    static SemanticSearchAvailability available() {
        new SemanticSearchAvailability(available: true)
    }

    static SemanticSearchAvailability unavailable(String reason) {
        new SemanticSearchAvailability(available: false, reason: reason)
    }
}
