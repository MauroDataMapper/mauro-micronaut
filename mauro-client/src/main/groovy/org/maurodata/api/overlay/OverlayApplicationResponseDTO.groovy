package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayApplicationResponseDTO {
    String rootDomainType
    UUID rootId
    List<UUID> overlayIds = []
    int matchedActions
    int appliedActions
}
