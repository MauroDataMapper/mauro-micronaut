package org.maurodata.persistence.terminology.dto

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class CodeSetTermDTO {

    UUID codeSetId
    UUID termId

    CodeSetTermDTO() {}
}
