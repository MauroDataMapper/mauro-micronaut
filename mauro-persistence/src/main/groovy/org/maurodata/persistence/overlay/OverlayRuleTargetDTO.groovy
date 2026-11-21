package org.maurodata.persistence.overlay

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class OverlayRuleTargetDTO {
    UUID overlayRuleId
    UUID itemId
    String itemDomainType
}
