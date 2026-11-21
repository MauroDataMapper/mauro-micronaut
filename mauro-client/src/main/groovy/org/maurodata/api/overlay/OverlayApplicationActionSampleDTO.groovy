package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayApplicationActionSampleDTO {
    UUID id
    String type
    String field
    String namespace
    String key
    String label
    String policy
    String source
    String valuePreview
    boolean valueTruncated
    String descriptionPreview
    boolean descriptionTruncated
}
