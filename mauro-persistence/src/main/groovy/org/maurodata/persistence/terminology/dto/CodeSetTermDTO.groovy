package org.maurodata.persistence.terminology.dto

import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class CodeSetTermDTO {

    UUID codeSetId
    UUID termId

    CodeSetTermDTO() {}
}
