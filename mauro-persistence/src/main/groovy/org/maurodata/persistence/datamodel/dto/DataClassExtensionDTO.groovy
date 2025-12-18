package org.maurodata.persistence.datamodel.dto

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataClassExtensionDTO {

    UUID dataClassId
    UUID extendedDataClassId

    DataClassExtensionDTO() {}
}
