package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayMatchResultDTO {
    UUID overlayId
    UUID ruleId
    List<UUID> actionIds = []
}
