package org.maurodata.domain.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayRule {
    UUID id = UUID.randomUUID()
    OverlayMatch match = new OverlayMatch()
    List<OverlayAction> actions = []
}
