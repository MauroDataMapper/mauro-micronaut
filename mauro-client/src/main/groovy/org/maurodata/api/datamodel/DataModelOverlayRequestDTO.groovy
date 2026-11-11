package org.maurodata.api.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class DataModelOverlayRequestDTO {
    UUID folderId
    String label
    String description
}
