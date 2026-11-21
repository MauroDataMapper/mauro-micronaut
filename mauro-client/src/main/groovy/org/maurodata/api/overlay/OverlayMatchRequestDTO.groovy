package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayMatchRequestDTO extends OverlayApplicationDTO {
    String itemDomainType
    UUID itemId
}
