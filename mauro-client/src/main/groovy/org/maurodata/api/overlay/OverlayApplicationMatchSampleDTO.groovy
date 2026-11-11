package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayApplicationMatchSampleDTO {
    UUID overlayId
    UUID overlayRuleId
    String itemDomainType
    UUID itemId
    String itemLabel
    String itemPath
    String selectorPath
    List<UUID> actionIds = []
    List<OverlayApplicationActionSampleDTO> actions = []
}
