package org.maurodata.domain.datamodel

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class IntersectsData {

    UUID sourceDataModelId
    UUID targetDataModelId

    /**
     * Data element ids from the source datamodel which intersect (by path) with data elements in the target datamodel.
     */
    @JsonInclude
    List<UUID> intersects

}
