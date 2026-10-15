package org.maurodata.domain.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

/** A change applied to each item selected by an Overlay rule. */
@CompileStatic
@Introspected
class OverlayAction {
    UUID id = UUID.randomUUID()
    /** 'value' (for item attributes) or 'metadata'. */
    String type
    /** Writable scalar property on the matched item; may be supplied with a leading '@'. */
    String field
    String namespace
    String key
    Object value
    /** always (default), ifEmpty, or add. */
    String policy = 'always'
    /** Provenance for the value supplied by this action. */
    String source
}
