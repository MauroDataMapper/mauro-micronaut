package org.maurodata.api.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class DataComponentCreateDTO {
    String label
    List<DataFlowSourceCreateDTO> consumes = []
    List<DataFlowSourceCreateDTO> produces = []
}
