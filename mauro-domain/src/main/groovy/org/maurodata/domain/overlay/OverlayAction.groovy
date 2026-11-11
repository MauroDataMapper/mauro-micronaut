package org.maurodata.domain.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.Item

/** A change applied to each item selected by an OverlayRule. */
@CompileStatic
@Introspected
class OverlayAction {
    UUID id = Item.timeBasedEpochGenerator.generate()
    /** 'value' (for item attributes), 'metadata', or 'annotation'. */
    String type
    /** Writable scalar property on the matched item; may be supplied with a leading '@'. */
    String field
    String namespace
    String key
    Object value
    /** Label for an annotation action. */
    String label
    /** Description for an annotation action. */
    String description
    /** always (default), ifEmpty, or add. */
    String policy = 'always'
    /** Provenance for the value supplied by this action. */
    String source

    void ensureId() {
        if (id == null) id = Item.timeBasedEpochGenerator.generate()
    }
}
