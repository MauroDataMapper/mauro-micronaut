package org.maurodata.domain.overlay

import groovy.transform.CompileStatic

/**
 * Policies supported by the current Overlay applier. The API and stored representation
 * remain strings; adding a policy here makes it available without changing that contract.
 */
@CompileStatic
enum OverlayActionPolicy {
    ALWAYS('always'),
    OVERWRITE_IF_EMPTY('overwriteIfEmpty'),
    ADD('add')

    final String identifier

    OverlayActionPolicy(String identifier) { this.identifier = identifier }

    static OverlayActionPolicy fromIdentifier(String identifier) {
        for (OverlayActionPolicy policy : values()) {
            if (policy.identifier == identifier) return policy
        }
        throw new IllegalArgumentException("Unsupported Overlay action policy: $identifier")
    }
}
