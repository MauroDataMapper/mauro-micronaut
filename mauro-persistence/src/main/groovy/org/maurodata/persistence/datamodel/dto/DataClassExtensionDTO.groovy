package org.maurodata.persistence.datamodel.dto

import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataClassExtensionDTO {

    UUID dataClassId
    UUID extendedDataClassId

    DataClassExtensionDTO() {}
}
