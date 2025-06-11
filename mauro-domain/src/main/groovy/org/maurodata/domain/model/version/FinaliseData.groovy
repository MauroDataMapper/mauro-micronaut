package org.maurodata.domain.model.version

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

/**
 * FinaliseData is a Data Transfer Object (DTO) that contains the information for an API finalisation request.
 * <p>
 * It contains the information about the type of change (Major, Minor or Patch), and the new version, and any
 * string-valued version tag that should be applied.  A text-valued 'changeNotice' may also describe the changes
 * between this version and the last, and is added to the history details of the model.
 */
@CompileStatic
@Introspected
class FinaliseData {

    VersionChangeType versionChangeType

    ModelVersion version

    String versionTag

    String changeNotice
}