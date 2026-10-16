package org.maurodata.domain.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.Path

/** Exact root-relative path selector for an OverlayRule. */
@CompileStatic
@Introspected
class OverlayMatch {
    /** Exact root-relative path, for example 'DM:Warehouse$main|DC:Customer'. */
    Path path = new Path('')
    /** Permit this selector to fan out across matching model versions/branches. */
    boolean allowMultipleModels = false
}
