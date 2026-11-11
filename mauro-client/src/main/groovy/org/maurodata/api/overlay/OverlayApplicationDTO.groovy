package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayApplicationDTO {
    String rootDomainType
    UUID rootId
    List<UUID> overlayIds = []
    /** Evaluate matching and write policies without persisting changes. */
    boolean dryRun = false
    /** Optional prior editable DataModel; only selector identities absent from it are considered. */
    UUID onlyNewItemsComparedWithRootId
}
