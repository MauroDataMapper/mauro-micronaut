package org.maurodata.api.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class OverlayAnnotationActionResultDTO {
    UUID overlayId
    UUID overlayRuleId
    UUID actionId
    String label
    String selectorPath
    int matchedCount
    int appliedCount
    int wouldApplyCount
    List<String> matchedPaths = []
    boolean matchedPathsTruncated
}
