package org.maurodata.persistence.facet.dto

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

/** Narrow database projection used while traversing ItemLink graphs. */
@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ItemLinkGraphRow {
    UUID id
    UUID targetId
    String type
    String path
}
