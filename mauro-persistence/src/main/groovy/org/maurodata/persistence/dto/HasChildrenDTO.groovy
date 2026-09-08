package org.maurodata.persistence.dto

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class HasChildrenDTO {

    UUID id
    Boolean hasChildren

    HasChildrenDTO() {}
}
